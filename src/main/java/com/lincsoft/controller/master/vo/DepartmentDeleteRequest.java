package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Department delete request VO.
 *
 * @param id Department ID
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record DepartmentDeleteRequest(
    @NotNull(message = "Department ID is required") Long id,
    @NotNull(message = "Version is required") Integer version) {}
