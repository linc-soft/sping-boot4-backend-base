package com.lincsoft.controller.master;

import com.lincsoft.controller.master.vo.RoleCreateRequest;
import com.lincsoft.controller.master.vo.RoleDeleteRequest;
import com.lincsoft.controller.master.vo.RoleInfoResponse;
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
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
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
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
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
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public Long createRole(@Valid @RequestBody RoleCreateRequest request) {
    return roleService.createRole(roleMapper.toEntity(request));
  }

  /**
   * Update an existing role.
   *
   * @param request Role update request
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void updateRole(@Valid @RequestBody RoleUpdateRequest request) {
    roleService.updateRole(roleMapper.toEntity(request));
  }

  /**
   * Delete a role.
   *
   * @param request Role delete request
   */
  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void deleteRole(@Valid @RequestBody RoleDeleteRequest request) {
    roleService.deleteRole(request.id(), request.version());
  }
}
