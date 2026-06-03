package com.lincsoft.controller.leave.vo;

import com.lincsoft.controller.common.vo.FileMetadataResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Leave page item response VO.
 *
 * @param id Leave ID
 * @param userId User ID
 * @param nickname Employee display name
 * @param startDate Leave start date
 * @param endDate Leave end date
 * @param leaveType Leave type code
 * @param duration Duration in days
 * @param reason Leave reason
 * @param status Leave status code
 * @param approverNickname Approver display name
 * @param approveTime Approval time
 * @param approveReason Approval/rejection reason
 * @param files Associated file metadata list
 * @author lincsoft
 * @since 2026-06-03
 */
public record LeavePageResponse(
    Long id,
    Long userId,
    String nickname,
    LocalDate startDate,
    LocalDate endDate,
    Integer leaveType,
    BigDecimal duration,
    String reason,
    Integer status,
    String approverNickname,
    LocalDateTime approveTime,
    String approveReason,
    List<FileMetadataResponse> files) {}
