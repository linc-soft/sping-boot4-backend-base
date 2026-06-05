package com.lincsoft.controller.master;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.constant.*;
import com.lincsoft.controller.master.vo.*;
import com.lincsoft.dto.master.EmployeeWithProfile;
import com.lincsoft.services.master.EmployeeService;
import com.lincsoft.services.master.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Employee controller.
 *
 * <p>Manages employee information which combines mst_user (login credentials) and mst_employee
 * (business information).
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

  private final EmployeeService employeeService;
  private final UserService userService;

  /**
   * Get employee by ID.
   *
   * @param id User ID
   * @return Employee response
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_READ.roleCode)")
  @GetMapping("/{id}")
  public EmployeeResponse getEmployee(@PathVariable Long id) {
    EmployeeWithProfile profile = employeeService.getEmployeeWithProfileByUserId(id);
    return employeeService.toResponse(profile.user(), profile.employee(), profile.roleIds());
  }

  /**
   * Get employee list.
   *
   * @param keyword search keyword (matches username or email)
   * @param status filter by status
   * @return list of employee responses
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_READ.roleCode)")
  @GetMapping
  public List<EmployeeListResponseItem> getEmployeeList(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status) {
    return employeeService.getEmployeeList(keyword, status);
  }

  /**
   * Get employee page.
   *
   * @param request page request with filters
   * @return IPage of employee responses
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_READ.roleCode)")
  @GetMapping("/page")
  public IPage<EmployeeListResponseItem> getEmployeePage(EmployeePageRequest request) {
    return employeeService.getEmployeePage(request);
  }

  /**
   * Create a new employee.
   *
   * @param request employee creation request
   * @return display name (username + nickname) of the created employee
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_WRITE.roleCode)")
  @PostMapping
  public String createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
    return employeeService.createEmployee(request);
  }

  /**
   * Update employee information.
   *
   * @param request employee update request
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_WRITE.roleCode)")
  @PutMapping
  public void updateEmployee(@Valid @RequestBody EmployeeUpdateRequest request) {
    employeeService.updateEmployee(request);
  }

  /**
   * Delete an employee.
   *
   * @param id User ID
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EMPLOYEE_WRITE.roleCode)")
  @DeleteMapping
  public void deleteEmployee(@RequestParam Long id) {
    employeeService.deleteEmployee(id);
  }
}
