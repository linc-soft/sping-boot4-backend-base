package com.lincsoft.controller.leave.vo;

import com.lincsoft.constant.LeaveStatus;
import jakarta.validation.constraints.*;

/**
 * Leave status update VO (approve/reject).
 *
 * @param id Leave ID
 * @param status New status (APPROVED or REJECTED)
 * @param reason Approval/rejection reason (required when rejecting)
 * @author lincsoft
 * @since 2026-06-03
 */
public record UpdateLeaveStatusRequest(
    @NotNull(message = "Leave ID is required") Long id,
    @NotNull(message = "Status is required") LeaveStatus status,
    String reason) {}
