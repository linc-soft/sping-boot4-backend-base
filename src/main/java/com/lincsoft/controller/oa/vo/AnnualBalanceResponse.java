package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Annual leave balance response VO.
 *
 * <p>Aggregates an employee's available annual leave as of today: the total available days plus the
 * per-batch breakdown of the active (non-expired) grant batches.
 *
 * @param employeeId Employee ID
 * @param totalAvailable Total available annual-leave days
 * @param batches Active grant batches, earliest grant date first
 * @author 林创科技
 * @since 2026-06-07
 */
public record AnnualBalanceResponse(
    Long employeeId, BigDecimal totalAvailable, List<AnnualBalanceBatch> batches) {}
