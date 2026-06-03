package com.lincsoft.controller.leave.vo;

import jakarta.validation.constraints.*;

/**
 * Annual leave initialization request VO.
 *
 * @param userId User ID (0 = initialize for all employees)
 * @param year Calendar year
 * @param force Force re-initialization if already exists
 * @author lincsoft
 * @since 2026-06-03
 */
public record InitAnnualLeaveRequest(
    @NotNull(message = "User ID is required") Long userId,
    @Min(value = 2025, message = "Year must be 2025 or later") Integer year,
    Boolean force) {}
