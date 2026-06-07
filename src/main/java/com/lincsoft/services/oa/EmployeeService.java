package com.lincsoft.services.oa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.Module;
import com.lincsoft.constant.OperationType;
import com.lincsoft.constant.SubModule;
import com.lincsoft.controller.oa.vo.EmployeePageRequest;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.oa.MstEmployee;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.oa.MstEmployeeMapper;
import com.lincsoft.services.master.UserService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee service.
 *
 * <p>Provides business logic for employee management. Creating an employee always provisions a
 * linked login account in the same transaction, so a failure on either side rolls back both.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {
  /** Employee mapper for database operations. */
  private final MstEmployeeMapper employeeMapper;

  /** User service for provisioning the linked login account. */
  private final UserService userService;

  /**
   * Get employee by ID.
   *
   * @param id Employee ID
   * @return MstEmployee entity
   * @throws BusinessException if the employee is not found
   */
  public MstEmployee getEmployeeById(Long id) {
    MstEmployee employee = employeeMapper.selectById(id);
    if (employee == null) {
      throw new BusinessException(MessageEnums.EMPLOYEE_NOT_FOUND);
    }
    return employee;
  }

  /**
   * Get employee page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of employees
   */
  public IPage<MstEmployee> getEmployeePage(EmployeePageRequest request) {
    QueryWrapper<MstEmployee> queryWrapper = new QueryWrapper<>();
    if (request.getRealName() != null && !request.getRealName().isBlank()) {
      queryWrapper.like("real_name", request.getRealName());
    }
    if (request.getEmployeeNo() != null && !request.getEmployeeNo().isBlank()) {
      queryWrapper.like("employee_no", request.getEmployeeNo());
    }
    if (request.getDeptId() != null) {
      queryWrapper.eq("dept_id", request.getDeptId());
    }
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      queryWrapper.eq("status", request.getStatus());
    }
    request.applySorting(
        queryWrapper,
        Set.of("id", "employee_no", "real_name", "dept_id", "hire_date", "create_at", "update_at"),
        "update_at");
    return employeeMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Create a new employee together with a linked login account.
   *
   * <p>Validates employee number uniqueness, provisions the login account via {@link
   * UserService#createUser} (which generates a password and sends the welcome email after commit),
   * then links the resulting user ID to the employee. The whole operation runs in one transaction:
   * if the employee insert fails, the account creation is rolled back as well.
   *
   * @param employee MstEmployee entity (userId is filled in by this method)
   * @param username Login username for the new account
   * @param roleIds Role IDs to assign to the new account (optional)
   * @return The created employee ID
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.CREATE,
      description = "Employee created: #{#employee.realName}")
  @Transactional(rollbackFor = Exception.class)
  public Long createEmployee(MstEmployee employee, String username, List<Integer> roleIds) {
    validateEmployeeNoUnique(employee.getEmployeeNo(), null);

    MstUser account = new MstUser();
    account.setUsername(username);
    account.setEmail(employee.getEmail());
    Long userId = userService.createUser(account, roleIds);

    employee.setUserId(userId);
    employeeMapper.insert(employee);
    return employee.getId();
  }

  /**
   * Update an existing employee profile.
   *
   * <p>Validates employee number uniqueness (excluding itself). The linked login account is not
   * touched here. Uses optimistic locking via the version field.
   *
   * @param employee MstEmployee entity with updated values
   * @throws BusinessException if the employee number is duplicate or optimistic lock fails
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.UPDATE,
      description = "Employee updated: #{#employee.realName}")
  @Transactional(rollbackFor = Exception.class)
  public void updateEmployee(MstEmployee employee) {
    getEmployeeById(employee.getId());
    validateEmployeeNoUnique(employee.getEmployeeNo(), employee.getId());

    int updated = employeeMapper.updateById(employee);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.EMPLOYEE_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Delete an employee.
   *
   * <p>Uses optimistic locking via an explicit version condition (logical delete does not apply the
   * {@code @Version} check). The linked login account is intentionally left untouched and must be
   * managed via the user management module.
   *
   * @param employee MstEmployee entity to delete
   * @param version Version for optimistic locking
   * @throws BusinessException if optimistic lock fails
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.DELETE,
      description = "Employee deleted: #{#employee.realName}")
  @Transactional(rollbackFor = Exception.class)
  public void deleteEmployee(MstEmployee employee, Integer version) {
    LambdaUpdateWrapper<MstEmployee> deleteWrapper = new LambdaUpdateWrapper<>();
    deleteWrapper.eq(MstEmployee::getId, employee.getId()).eq(MstEmployee::getVersion, version);
    int deleted = employeeMapper.delete(deleteWrapper);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.EMPLOYEE_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Find the employee linked to the given login account.
   *
   * @param userId Login account ID (mst_user.id)
   * @return the linked employee, or null if none
   */
  public MstEmployee findByUserId(Long userId) {
    QueryWrapper<MstEmployee> qw = new QueryWrapper<>();
    qw.eq("user_id", userId);
    return employeeMapper.selectOne(qw);
  }

  /**
   * Count active employees belonging to the given department.
   *
   * @param deptId Department ID
   * @return number of employees in the department
   */
  public long countByDeptId(Long deptId) {
    QueryWrapper<MstEmployee> qw = new QueryWrapper<>();
    qw.eq("dept_id", deptId);
    return employeeMapper.selectCount(qw);
  }

  /**
   * Count active employees holding the given position.
   *
   * @param positionId Position ID
   * @return number of employees with the position
   */
  public long countByPositionId(Long positionId) {
    QueryWrapper<MstEmployee> qw = new QueryWrapper<>();
    qw.eq("position_id", positionId);
    return employeeMapper.selectCount(qw);
  }

  /**
   * Validate that the employee number is unique among non-deleted employees.
   *
   * @param employeeNo Employee number to check (skipped when null or blank)
   * @param excludeId Employee ID to exclude from the check (null when creating)
   * @throws BusinessException if the employee number already exists
   */
  private void validateEmployeeNoUnique(String employeeNo, Long excludeId) {
    if (employeeNo == null || employeeNo.isBlank()) {
      return;
    }
    QueryWrapper<MstEmployee> qw = new QueryWrapper<>();
    qw.eq("employee_no", employeeNo);
    if (excludeId != null) {
      qw.ne("id", excludeId);
    }
    if (employeeMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.EMPLOYEE_NO_EXISTS);
    }
  }
}
