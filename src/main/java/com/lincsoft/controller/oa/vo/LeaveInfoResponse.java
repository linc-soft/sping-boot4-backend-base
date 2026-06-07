package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Leave request info response VO.
 *
 * @param id Leave request ID
 * @param employeeId Applicant employee ID
 * @param leaveType Leave type
 * @param startTime Leave start time
 * @param endTime Leave end time
 * @param days Number of leave days
 * @param reason Leave reason
 * @param status Status (0 pending / 1 approved / 2 rejected / 3 withdrawn)
 * @param processInstanceId Flowable process instance ID
 * @param approverId Resolved approver employee ID
 * @param approvalComment Approver comment
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeaveInfoResponse(
    Long id,
    Long employeeId,
    String leaveType,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal days,
    String reason,
    String status,
    String processInstanceId,
    Long approverId,
    String approvalComment,
    Integer version) {}
