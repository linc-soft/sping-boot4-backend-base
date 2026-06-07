package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Leave withdraw request VO.
 *
 * @param id Leave request ID
 * @author 林创科技
 * @since 2026-06-07
 */
public record LeaveWithdrawRequest(@NotNull(message = "Leave request ID is required") Long id) {}
