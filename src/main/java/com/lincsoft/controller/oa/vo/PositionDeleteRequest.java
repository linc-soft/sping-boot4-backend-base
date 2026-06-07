package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Position delete request VO.
 *
 * @param id Position ID
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record PositionDeleteRequest(
    @NotNull(message = "Position ID is required") Long id,
    @NotNull(message = "Version is required") Integer version) {}
