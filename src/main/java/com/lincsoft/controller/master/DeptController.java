package com.lincsoft.controller.master;

import com.lincsoft.entity.master.MstDept;
import com.lincsoft.services.master.DeptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Department controller.
 *
 * <p>Provides endpoints for department tree CRUD operations.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/api/depts")
@RequiredArgsConstructor
public class DeptController {

  private final DeptService deptService;

  /**
   * Get the full department tree (flat list ordered by parent_id, sort_order).
   *
   * @return list of all departments
   */
  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
  public List<MstDept> getDeptTree() {
    return deptService.getDeptTree();
  }

  /**
   * Get a department by ID.
   *
   * @param id department ID
   * @return department entity
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
  public MstDept getDept(@PathVariable Long id) {
    return deptService.getDeptById(id);
  }

  /**
   * Create a new department.
   *
   * @param dept department entity
   * @return created department ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public Long createDept(@Valid @RequestBody MstDept dept) {
    return deptService.createDept(dept);
  }

  /**
   * Update an existing department.
   *
   * @param dept department entity with updated values
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void updateDept(@Valid @RequestBody MstDept dept) {
    deptService.updateDept(dept);
  }

  /**
   * Delete a department.
   *
   * @param id department ID
   * @param version version for optimistic locking
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void deleteDept(@PathVariable Long id, @RequestParam @NotNull Integer version) {
    deptService.deleteDept(id, version);
  }
}
