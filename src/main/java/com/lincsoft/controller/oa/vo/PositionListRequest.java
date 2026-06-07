package com.lincsoft.controller.oa.vo;

/**
 * Position list request VO.
 *
 * @param positionName Position name (partial match, optional)
 * @param status Status (exact match, optional)
 * @author 林创科技
 * @since 2026-06-07
 */
public record PositionListRequest(String positionName, String status) {}
