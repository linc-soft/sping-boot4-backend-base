package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pending leave approval task response VO.
 *
 * <p>Combines the Flowable task identity with the underlying leave request business data so the
 * approver can review and act in a single view.
 *
 * @param leaveId Leave request ID
 * @param employeeId Applicant employee ID
 * @param leaveType Leave type
 * @param startDate Leave start date
 * @param startPeriod Leave start period ('0' = AM, '1' = PM)
 * @param endDate Leave end date
 * @param endPeriod Leave end period ('0' = AM, '1' = PM)
 * @param days Number of leave days
 * @param reason Leave reason
 * @param createAt Submission time
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeaveTaskResponseItem(
    Long leaveId,
    Long employeeId,
    String leaveType,
    LocalDate startDate,
    String startPeriod,
    LocalDate endDate,
    String endPeriod,
    BigDecimal days,
    String reason,
    LocalDateTime createAt) {}
