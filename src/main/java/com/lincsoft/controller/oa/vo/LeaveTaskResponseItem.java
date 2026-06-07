package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
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
 * @param startTime Leave start time
 * @param endTime Leave end time
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
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal days,
    String reason,
    LocalDateTime createAt) {}
