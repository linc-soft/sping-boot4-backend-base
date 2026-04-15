package com.lincsoft.controller.master;

import com.lincsoft.controller.master.vo.RoleCreateRequest;
import com.lincsoft.controller.master.vo.RoleUpdateRequest;
import com.lincsoft.mapstruct.RoleMapper;
import com.lincsoft.services.master.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
   * Create a new role.
   *
   * @param request Role create request
   * @return created role ID
   */
  @PostMapping
  public Long createRole(@Valid @RequestBody RoleCreateRequest request) {
    return roleService.createRole(roleMapper.toEntity(request));
  }

  /**
   * Update an existing role.
   *
   * @param request Role update request
   */
  @PutMapping
  public void updateRole(@Valid @RequestBody RoleUpdateRequest request) {
    roleService.updateRole(roleMapper.toEntityForUpdate(request));
  }
}
