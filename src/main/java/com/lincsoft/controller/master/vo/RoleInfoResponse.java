package com.lincsoft.controller.master.vo;

import java.util.List;

/**
 * Role info response VO.
 *
 * @param id Role ID
 * @param roleName Role name
 * @param roleCode Role code
 * @param description Role description
 * @param parentRoleIds Direct parent role IDs that this role inherits from
 * @param version Version
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleInfoResponse(
    Long id,
    String roleName,
    String roleCode,
    String description,
    List<Long> parentRoleIds,
    Integer version) {}
