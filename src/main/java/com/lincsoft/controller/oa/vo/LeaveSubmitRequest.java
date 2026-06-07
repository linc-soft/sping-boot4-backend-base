package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Leave submit request VO.
 *
 * <p>The applicant employee is resolved from the authenticated user, so it is not part of the
 * request body.
 *
 * @param leaveType Leave type (1 annual / 2 sick / 3 personal / 4 marriage / 5 maternity / 9 other)
 * @param startTime Leave start time
 * @param endTime Leave end time
 * @param days Number of leave days
 * @param reason Leave reason
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeaveSubmitRequest(
    @NotNull(message = "Leave type is required")
        @Size(max = 2, message = "Leave type must be at most 2 characters")
        String leaveType,
    @NotNull(message = "Start time is required") LocalDateTime startTime,
    @NotNull(message = "End time is required") LocalDateTime endTime,
    @NotNull(message = "Days is required") BigDecimal days,
    @Size(max = 500, message = "Reason must be at most 500 characters") String reason) {}
