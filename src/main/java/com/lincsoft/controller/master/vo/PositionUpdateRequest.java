package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Position update request VO.
 *
 * @param id Position ID
 * @param positionName Position name
 * @param positionCode Position code
 * @param sortOrder Sort order (optional)
 * @param status Status (0 disabled / 1 enabled, optional)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record PositionUpdateRequest(
    @NotNull(message = "Position ID is required") Long id,
    @NotBlank(message = "Position name is required")
        @Size(max = 64, message = "Position name must be at most 64 characters")
        String positionName,
    @Size(max = 64, message = "Position code must be at most 64 characters") String positionCode,
    Integer sortOrder,
    @Size(max = 1, message = "Status must be a single character") String status,
    @NotNull(message = "Version is required") Integer version) {}
