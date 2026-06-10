package com.lincsoft.controller.master.vo;

/**
 * Position info response VO.
 *
 * @param id Position ID
 * @param positionName Position name
 * @param positionCode Position code
 * @param sortOrder Sort order
 * @param status Status (0 disabled / 1 enabled)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record PositionInfoResponse(
    Long id,
    String positionName,
    String positionCode,
    Integer sortOrder,
    String status,
    Integer version) {}
