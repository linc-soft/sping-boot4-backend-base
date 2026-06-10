package com.lincsoft.controller.oa.vo;

/**
 * Department info response VO.
 *
 * @param id Department ID
 * @param deptName Department name
 * @param deptCode Department code
 * @param parentId Parent department ID (0 = top level)
 * @param leaderUserId Department head user ID
 * @param sortOrder Sort order among siblings
 * @param status Status (0 disabled / 1 enabled)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record DepartmentInfoResponse(
    Long id,
    String deptName,
    String deptCode,
    Long parentId,
    Long leaderUserId,
    Integer sortOrder,
    String status,
    Integer version) {}
