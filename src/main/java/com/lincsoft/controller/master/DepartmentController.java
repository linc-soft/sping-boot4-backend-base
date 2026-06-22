package com.lincsoft.controller.master;

import com.lincsoft.controller.master.vo.DepartmentCreateRequest;
import com.lincsoft.controller.master.vo.DepartmentDeleteRequest;
import com.lincsoft.controller.master.vo.DepartmentInfoResponse;
import com.lincsoft.controller.master.vo.DepartmentTreeResponse;
import com.lincsoft.controller.master.vo.DepartmentUpdateRequest;
import com.lincsoft.mapstruct.DepartmentMapper;
import com.lincsoft.services.master.DepartmentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Department controller.
 *
 * <p>Provides endpoints for department CRUD operations and tree retrieval.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/master/departments")
@RequiredArgsConstructor
public class DepartmentController {

  /** Department service. */
  private final DepartmentService departmentService;

  /** Department mapper for converting between VO and entity. */
  private final DepartmentMapper departmentMapper;

  /**
   * Get department by ID.
   *
   * @param id Department ID
   * @return Department info response
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).VIEW_DEPARTMENT.roleCode)")
  public DepartmentInfoResponse getDepartment(@PathVariable Long id) {
    return departmentMapper.toInfoResponse(departmentService.getDepartmentById(id));
  }

  /**
   * Get the full department tree.
   *
   * @return List of top-level department tree nodes
   */
  @GetMapping("/tree")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LIST_DEPARTMENT.roleCode)")
  public List<DepartmentTreeResponse> getDepartmentTree() {
    return departmentService.getDepartmentTree();
  }

  /**
   * Create a new department.
   *
   * @param request Department create request
   * @return created department ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).CREATE_DEPARTMENT.roleCode)")
  public Long createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
    return departmentService.createDepartment(departmentMapper.toEntity(request));
  }

  /**
   * Update an existing department.
   *
   * @param request Department update request
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).UPDATE_DEPARTMENT.roleCode)")
  public void updateDepartment(@Valid @RequestBody DepartmentUpdateRequest request) {
    departmentService.updateDepartment(departmentMapper.toEntity(request));
  }

  /**
   * Delete a department.
   *
   * @param request Department delete request
   */
  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).DELETE_DEPARTMENT.roleCode)")
  public void deleteDepartment(@Valid @RequestBody DepartmentDeleteRequest request) {
    departmentService.deleteDepartment(
        departmentService.getDepartmentById(request.id()), request.version());
  }
}
