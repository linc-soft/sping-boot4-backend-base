package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Department update request VO.
 *
 * @param id Department ID
 * @param deptName Department name
 * @param deptCode Department code
 * @param parentId Parent department ID (0 = top level)
 * @param leaderEmployeeId Department head employee ID (optional)
 * @param sortOrder Sort order among siblings (optional)
 * @param status Status (0 disabled / 1 enabled, optional)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record DepartmentUpdateRequest(
    @NotNull(message = "Department ID is required") Long id,
    @NotBlank(message = "Department name is required")
        @Size(max = 64, message = "Department name must be at most 64 characters")
        String deptName,
    @Size(max = 64, message = "Department code must be at most 64 characters") String deptCode,
    Long parentId,
    Long leaderEmployeeId,
    Integer sortOrder,
    @Size(max = 1, message = "Status must be a single character") String status,
    @NotNull(message = "Version is required") Integer version) {}
