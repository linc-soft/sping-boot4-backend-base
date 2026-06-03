package com.lincsoft.controller.leave.vo;

import com.lincsoft.constant.LeaveType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Leave request create/update VO.
 *
 * @param userId User ID
 * @param startDate Leave start date (yyyy-MM-dd)
 * @param endDate Leave end date (yyyy-MM-dd)
 * @param leaveType Leave type
 * @param duration Duration in days
 * @param reason Reason
 * @param fileIds File upload IDs to associate
 * @author lincsoft
 * @since 2026-06-03
 */
public record SaveLeaveRequest(
    @NotNull(message = "User ID is required") Long userId,
    @NotBlank(message = "Start date is required") String startDate,
    @NotBlank(message = "End date is required") String endDate,
    @NotNull(message = "Leave type is required") LeaveType leaveType,
    @NotNull(message = "Duration is required")
        @DecimalMin(value = "0.5", message = "Duration must be at least 0.5 days")
        BigDecimal duration,
    @NotBlank(message = "Reason is required") String reason,
    List<Long> fileIds) {}
