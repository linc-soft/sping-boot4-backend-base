package com.lincsoft.controller.master;

import com.lincsoft.entity.master.MstRoleDataScope;
import com.lincsoft.services.master.RoleDataScopeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Role data scope controller.
 *
 * <p>Provides endpoints for managing department-based data scope configurations for roles.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/api/role-data-scopes")
@RequiredArgsConstructor
public class RoleDataScopeController {

  private final RoleDataScopeService roleDataScopeService;

  /**
   * Get all data scope configurations for a role.
   *
   * @param roleId role ID
   * @return list of data scope configurations
   */
  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public List<MstRoleDataScope> getRoleDataScopeList(@RequestParam @NotNull Long roleId) {
    return roleDataScopeService.getRoleDataScopeList(roleId);
  }

  /**
   * Create a new role data scope configuration.
   *
   * @param scope data scope configuration
   * @return created record ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public Long createRoleDataScope(@Valid @RequestBody MstRoleDataScope scope) {
    return roleDataScopeService.createRoleDataScope(scope);
  }

  /**
   * Update an existing role data scope configuration.
   *
   * @param scope data scope configuration with updated values
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void updateRoleDataScope(@Valid @RequestBody MstRoleDataScope scope) {
    roleDataScopeService.updateRoleDataScope(scope);
  }

  /**
   * Delete a role data scope configuration.
   *
   * @param id record ID
   * @param version version for optimistic locking
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void deleteRoleDataScope(@PathVariable Long id, @RequestParam @NotNull Integer version) {
    roleDataScopeService.deleteRoleDataScope(id, version);
  }
}
