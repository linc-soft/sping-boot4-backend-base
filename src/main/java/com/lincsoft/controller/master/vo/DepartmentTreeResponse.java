package com.lincsoft.controller.master.vo;

import java.util.List;

/**
 * Department tree node response VO.
 *
 * <p>Each node carries its direct children, forming a nested tree. Top-level nodes have {@code
 * parentId} equal to 0.
 *
 * @param id Department ID
 * @param deptName Department name
 * @param deptCode Department code
 * @param parentId Parent department ID (0 = top level)
 * @param leaderUserId Department head user ID
 * @param sortOrder Sort order among siblings
 * @param status Status (0 disabled / 1 enabled)
 * @param version Version for optimistic locking
 * @param children Direct child departments
 * @author 林创科技
 * @since 2026-06-07
 */
public record DepartmentTreeResponse(
    Long id,
    String deptName,
    String deptCode,
    Long parentId,
    Long leaderUserId,
    Integer sortOrder,
    String status,
    Integer version,
    List<DepartmentTreeResponse> children) {}
