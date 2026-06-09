package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Leave request page item response VO.
 *
 * @param id Leave request ID
 * @param employeeId Applicant employee ID
 * @param leaveType Leave type
 * @param startDate Leave start date
 * @param startPeriod Leave start period (0 = AM, 1 = PM)
 * @param endDate Leave end date
 * @param endPeriod Leave end period (0 = AM, 1 = PM)
 * @param days Number of leave days
 * @param status Status (0 pending / 1 approved / 2 rejected / 3 withdrawn)
 * @param approverId Resolved approver employee ID
 * @param createAt Submission time
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeavePageResponseItem(
    Long id,
    Long employeeId,
    String leaveType,
    LocalDate startDate,
    String startPeriod,
    LocalDate endDate,
    String endPeriod,
    BigDecimal days,
    String status,
    Long approverId,
    LocalDateTime createAt,
    Integer version) {}
