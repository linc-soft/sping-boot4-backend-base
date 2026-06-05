package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.*;
import com.lincsoft.constant.Module;
import com.lincsoft.controller.master.vo.*;
import com.lincsoft.dto.master.EmployeeWithProfile;
import com.lincsoft.entity.master.Employee;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.EmployeeMapper;
import com.lincsoft.mapper.master.MstUserMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee service for managing employee information.
 *
 * <p>Handles CRUD operations for employees, which consist of both a {@link MstUser} (login
 * credentials) and an {@link Employee} (business information) record linked 1:1.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

  private final MstUserMapper userMapper;
  private final EmployeeMapper employeeMapper;
  private final MstUserRoleMapper userRoleMapper;
  private final RoleService roleService;
  private final UserService userService;
  private final com.lincsoft.mapstruct.EmployeeMapper employeeMapperConvert;

  /**
   * Get employee by user ID (with user and role info).
   *
   * @param userId the user ID
   * @return EmployeeWithProfile DTO
   * @throws BusinessException if employee not found
   */
  public EmployeeWithProfile getEmployeeWithProfileByUserId(Long userId) {
    MstUser user = userService.getUserById(userId);
    Employee employee = getEmployeeByUserId(userId);
    List<Long> roleIds =
        roleService.getRoleListByUserId(userId).stream().map(MstRole::getId).toList();
    return new EmployeeWithProfile(user, employee, roleIds);
  }

  /**
   * Get employee list.
   *
   * @return list of EmployeeListResponseItem
   */
  public List<EmployeeListResponseItem> getEmployeeList(String keyword, String status) {
    LambdaQueryWrapper<MstUser> queryWrapper = new LambdaQueryWrapper<>();
    if (keyword != null && !keyword.isBlank()) {
      queryWrapper.and(
          w -> w.like(MstUser::getUsername, keyword).or().like(MstUser::getEmail, keyword));
    }
    if (status != null && !status.isBlank()) {
      queryWrapper.eq(MstUser::getStatus, status);
    }
    queryWrapper.orderByDesc(MstUser::getUpdateAt);
    List<MstUser> users = userMapper.selectList(queryWrapper);
    return users.stream().map(this::toListResponseItem).collect(Collectors.toList());
  }

  /**
   * Get employee page.
   *
   * @param request page request with filters
   * @return IPage of EmployeeListResponseItem
   */
  public IPage<EmployeeListResponseItem> getEmployeePage(EmployeePageRequest request) {
    LambdaQueryWrapper<MstUser> queryWrapper = new LambdaQueryWrapper<>();
    if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
      queryWrapper.and(
          w ->
              w.like(MstUser::getUsername, request.getKeyword())
                  .or()
                  .like(MstUser::getEmail, request.getKeyword()));
    }
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      queryWrapper.eq(MstUser::getStatus, request.getStatus());
    }
    queryWrapper.orderByDesc(MstUser::getUpdateAt);
    IPage<MstUser> userPage = userMapper.selectPage(request.toPage(), queryWrapper);
    return userPage.convert(this::toListResponseItem);
  }

  /**
   * Create a new employee (mst_user + mst_employee + default roles).
   *
   * <p>Delegates user creation to {@link UserService#createUser(MstUser, List)} which handles
   * username/email uniqueness validation, password generation, status defaulting, role assignment,
   * and welcome email sending.
   *
   * @param request the creation request
   * @return display name (username + nickname) of the created employee
   */
  @OperationLog(
      module = Module.MASTER,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.CREATE,
      description = "Employee created: #{#result}")
  @Transactional(rollbackFor = Exception.class)
  public String createEmployee(EmployeeCreateRequest request) {
    MstUser user = employeeMapperConvert.toUserEntity(request);
    Long userId = userService.createUser(user, request.roleIds());

    Employee employee = employeeMapperConvert.toEmployeeEntity(request);
    employee.setUserId(userId);
    employee.setSex(request.sex() != null ? request.sex().getCode() : 0);
    employee.setHiredDate(
        request.hiredDate() != null ? LocalDate.parse(request.hiredDate()) : null);
    employeeMapper.insert(employee);

    return formatDisplayName(request.username(), request.nickname());
  }

  /**
   * Update employee information.
   *
   * <p>Delegates user update to {@link UserService#updateUser(MstUser, List)} which handles
   * username immutability check, email uniqueness validation, password encryption, optimistic
   * locking, and role synchronization.
   *
   * @param request the update request
   */
  @OperationLog(
      module = Module.MASTER,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.UPDATE,
      description = "Employee updated: #{#result}")
  @Transactional(rollbackFor = Exception.class)
  public String updateEmployee(EmployeeUpdateRequest request) {
    MstUser existingUser = userService.getUserById(request.id());
    MstUser user = employeeMapperConvert.toUserEntity(request);
    user.setUsername(existingUser.getUsername());
    userService.updateUser(user, request.roleIds());

    Employee employee = getEmployeeByUserId(request.id());
    if (request.nickname() != null) {
      employee.setNickname(request.nickname());
    }
    if (request.mobile() != null) {
      employee.setMobile(request.mobile());
    }
    if (request.sex() != null) {
      employee.setSex(request.sex().getCode());
    }
    if (request.hiredDate() != null) {
      employee.setHiredDate(LocalDate.parse(request.hiredDate()));
    }
    if (request.remark() != null) {
      employee.setRemark(request.remark());
    }
    employeeMapper.updateById(employee);

    return formatDisplayName(existingUser.getUsername(), employee.getNickname());
  }

  /**
   * Delete an employee (cascade delete mst_user + mst_employee + role assignments).
   *
   * @param userId the user ID
   */
  @OperationLog(
      module = Module.MASTER,
      subModule = SubModule.EMPLOYEE,
      type = OperationType.DELETE,
      description = "Employee deleted: #{#result}")
  @Transactional(rollbackFor = Exception.class)
  public String deleteEmployee(Long userId) {
    MstUser existingUser = userService.getUserById(userId);
    Employee existingEmployee = getEmployeeByUserId(userId);
    String displayName =
        formatDisplayName(existingUser.getUsername(), existingEmployee.getNickname());

    LambdaQueryWrapper<Employee> empQuery = new LambdaQueryWrapper<>();
    empQuery.eq(Employee::getUserId, userId);
    employeeMapper.delete(empQuery);

    LambdaQueryWrapper<MstUserRole> roleQuery = new LambdaQueryWrapper<>();
    roleQuery.eq(MstUserRole::getUserId, userId);
    userRoleMapper.delete(roleQuery);

    userMapper.deleteById(userId);
    return displayName;
  }

  private String formatDisplayName(String username, String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return username;
    }
    return username + " (" + nickname + ")";
  }

  /** Get Employee entity by userId. */
  public Employee getEmployeeByUserId(Long userId) {
    LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Employee::getUserId, userId);
    Employee employee = employeeMapper.selectOne(queryWrapper);
    if (employee == null) {
      throw new BusinessException(MessageEnums.EMPLOYEE_NOT_FOUND);
    }
    return employee;
  }

  public EmployeeResponse toResponse(MstUser user, Employee employee, List<Long> roleIds) {
    return new EmployeeResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getStatus(),
        roleIds,
        user.getVersion(),
        employee.getNickname(),
        employee.getMobile(),
        employee.getSex() != null ? employee.getSex() : 0,
        employee.getHiredDate(),
        employee.getRemark(),
        null,
        null,
        null,
        null);
  }

  private EmployeeListResponseItem toListResponseItem(MstUser user) {
    return new EmployeeListResponseItem(
        user.getId(),
        user.getUsername(),
        null,
        null,
        null,
        user.getStatus(),
        user.getUpdateBy(),
        user.getUpdateAt(),
        user.getVersion(),
        null,
        null,
        null,
        null);
  }
}
