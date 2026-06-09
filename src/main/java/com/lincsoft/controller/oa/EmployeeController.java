package com.lincsoft.controller.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.oa.vo.EmployeeCreateRequest;
import com.lincsoft.controller.oa.vo.EmployeeDeleteRequest;
import com.lincsoft.controller.oa.vo.EmployeeInfoResponse;
import com.lincsoft.controller.oa.vo.EmployeePageRequest;
import com.lincsoft.controller.oa.vo.EmployeePageResponseItem;
import com.lincsoft.controller.oa.vo.EmployeeUpdateRequest;
import com.lincsoft.entity.oa.MstEmployee;
import com.lincsoft.mapstruct.EmployeeMapper;
import com.lincsoft.services.oa.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Employee controller.
 *
 * <p>Provides endpoints for employee CRUD operations. Creating an employee also provisions a linked
 * login account.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

  /** Employee service. */
  private final EmployeeService employeeService;

  /** Employee mapper for converting between VO and entity. */
  private final EmployeeMapper employeeMapper;

  /**
   * Get employee by ID.
   *
   * @param id Employee ID
   * @return Employee info response
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_READ.roleCode)")
  public EmployeeInfoResponse getEmployee(@PathVariable Long id) {
    return employeeMapper.toInfoResponse(employeeService.getEmployeeById(id));
  }

  /**
   * Get employee page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of employee items
   */
  @GetMapping("/page")
  @PreAuthorize(
      "hasAnyRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_READ.roleCode, T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  public IPage<EmployeePageResponseItem> getEmployeePage(EmployeePageRequest request) {
    return employeeService.getEmployeePage(request).convert(employeeMapper::toPageResponseItem);
  }

  /**
   * Create a new employee together with a linked login account.
   *
   * @param request Employee create request
   * @return created employee ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_WRITE.roleCode)")
  public Long createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
    MstEmployee employee = employeeMapper.toEntity(request);
    return employeeService.createEmployee(employee, request.username(), request.roleIds());
  }

  /**
   * Update an existing employee.
   *
   * @param request Employee update request
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_WRITE.roleCode)")
  public void updateEmployee(@Valid @RequestBody EmployeeUpdateRequest request) {
    employeeService.updateEmployee(employeeMapper.toEntity(request));
  }

  /**
   * Delete an employee.
   *
   * @param request Employee delete request
   */
  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_DELETE.roleCode)")
  public void deleteEmployee(@Valid @RequestBody EmployeeDeleteRequest request) {
    employeeService.deleteEmployee(
        employeeService.getEmployeeById(request.id()), request.version());
  }
}
