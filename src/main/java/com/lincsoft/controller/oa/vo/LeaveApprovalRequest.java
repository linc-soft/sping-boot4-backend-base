package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Leave approval request VO (approve or reject).
 *
 * @param id Leave request ID
 * @param approved Approval decision (true = approve, false = reject)
 * @param comment Approver comment
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeaveApprovalRequest(
    @NotNull(message = "Leave request ID is required") Long id,
    @NotNull(message = "Approval decision is required") Boolean approved,
    @Size(max = 500, message = "Comment must be at most 500 characters") String comment) {}
