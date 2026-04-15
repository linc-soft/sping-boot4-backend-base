package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotNull;

/**
 * User delete request VO.
 *
 * @param id User ID
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserDeleteRequest(
    @NotNull(message = "User ID is required") Long id,
    @NotNull(message = "Version is required") Integer version) {}
