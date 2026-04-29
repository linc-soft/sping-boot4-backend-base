package com.lincsoft.controller.master;

import com.lincsoft.controller.master.vo.RoleCreateRequest;
import com.lincsoft.controller.master.vo.RoleDeleteRequest;
import com.lincsoft.controller.master.vo.RoleInfoResponse;
import com.lincsoft.controller.master.vo.RoleInheritanceRequest;
import com.lincsoft.controller.master.vo.RoleListRequest;
import com.lincsoft.controller.master.vo.RoleListResponseItem;
import com.lincsoft.controller.master.vo.RoleUpdateRequest;
import com.lincsoft.mapstruct.RoleMapper;
import com.lincsoft.services.master.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Role controller.
 *
 * <p>Provides endpoints for role CRUD operations.
 *
 * @author 林创科技
 * @since 2026-04-14
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

  /** Role service */
  private final RoleService roleService;

  /** Role mapper for converting between VO and entity. */
  private final RoleMapper roleMapper;

  /**
   * Get role by ID.
   *
   * @param id Role ID
   * @return Role info response
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public RoleInfoResponse getRole(@PathVariable Long id) {
    return roleMapper.toInfoResponse(roleService.getRoleById(id));
  }

  /**
   * Get role list by query conditions.
   *
   * @param request Query parameters (roleName, roleCode, description)
   * @return List of role items
   */
  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public List<RoleListResponseItem> getRoleList(RoleListRequest request) {
    return roleMapper.toListResponse(
        roleService.getRoleList(request.roleName(), request.roleCode(), request.description()));
  }

  /**
   * Create a new role.
   *
   * @param request Role create request
   * @return created role ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_NEW.roleCode)")
  public Long createRole(@Valid @RequestBody RoleCreateRequest request) {
    return roleService.createRole(roleMapper.toEntity(request));
  }

  /**
   * Update an existing role.
   *
   * @param request Role update request
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_EDIT.roleCode)")
  public void updateRole(@Valid @RequestBody RoleUpdateRequest request) {
    roleService.updateRole(roleMapper.toEntity(request));
  }

  /**
   * Delete a role.
   *
   * @param request Role delete request
   */
  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_DEL.roleCode)")
  public void deleteRole(@Valid @RequestBody RoleDeleteRequest request) {
    roleService.deleteRole(request.id(), request.version());
  }

  // ========== Role Inheritance Endpoints ==========

  /**
   * Add role inheritance relationship.
   *
   * @param request Role inheritance request (childRoleId, parentRoleId)
   */
  @PostMapping("/inheritance")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_EDIT.roleCode)")
  public void addRoleInheritance(@Valid @RequestBody RoleInheritanceRequest request) {
    roleService.addRoleInheritance(request.childRoleId(), request.parentRoleId());
  }

  /**
   * Remove role inheritance relationship.
   *
   * @param request Role inheritance request (childRoleId, parentRoleId)
   */
  @DeleteMapping("/inheritance")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_EDIT.roleCode)")
  public void removeRoleInheritance(@Valid @RequestBody RoleInheritanceRequest request) {
    roleService.removeRoleInheritance(request.childRoleId(), request.parentRoleId());
  }

  /**
   * Get parent roles of a given role.
   *
   * @param id Role ID
   * @return List of parent roles
   */
  @GetMapping("/{id}/parents")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public List<RoleListResponseItem> getParentRoles(@PathVariable Long id) {
    return roleMapper.toListResponse(roleService.getParentRoles(id));
  }

  /**
   * Get child roles of a given role.
   *
   * @param id Role ID
   * @return List of child roles
   */
  @GetMapping("/{id}/children")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public List<RoleListResponseItem> getChildRoles(@PathVariable Long id) {
    return roleMapper.toListResponse(roleService.getChildRoles(id));
  }
}
