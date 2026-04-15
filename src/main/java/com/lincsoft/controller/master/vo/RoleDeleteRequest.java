package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Role delete request VO.
 *
 * @param id Role ID
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleDeleteRequest(
    @NotNull(message = "Role ID is required") Long id,
    @NotNull(message = "Version is required") Integer version) {}
