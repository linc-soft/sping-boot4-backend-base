package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Role inheritance request VO.
 *
 * @param childRoleId Child role ID (the role that inherits)
 * @param parentRoleId Parent role ID (the role being inherited)
 * @author 林创科技
 * @since 2026-04-27
 */
public record RoleInheritanceRequest(
    @NotNull(message = "Child role ID is required") Long childRoleId,
    @NotNull(message = "Parent role ID is required") Long parentRoleId) {}
