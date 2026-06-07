package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Position create request VO.
 *
 * @param positionName Position name
 * @param positionCode Position code
 * @param sortOrder Sort order (optional)
 * @param status Status (0 disabled / 1 enabled, optional)
 * @author 林创科技
 * @since 2026-06-07
 */
public record PositionCreateRequest(
    @NotBlank(message = "Position name is required")
        @Size(max = 64, message = "Position name must be at most 64 characters")
        String positionName,
    @Size(max = 64, message = "Position code must be at most 64 characters") String positionCode,
    Integer sortOrder,
    @Size(max = 1, message = "Status must be a single character") String status) {}
