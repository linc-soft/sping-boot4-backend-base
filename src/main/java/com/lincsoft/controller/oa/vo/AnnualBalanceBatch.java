package com.lincsoft.controller.oa.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Annual leave balance batch detail VO.
 *
 * <p>One active grant batch within an employee's annual-leave balance.
 *
 * @param grantDate Grant date (employment anniversary)
 * @param expireDate Expiry date (grantDate + 24 months)
 * @param grantedDays Days granted in this batch
 * @param usedDays Days already consumed from this batch
 * @param remainingDays Days remaining in this batch
 * @author 林创科技
 * @since 2026-06-07
 */
public record AnnualBalanceBatch(
    LocalDate grantDate,
    LocalDate expireDate,
    BigDecimal grantedDays,
    BigDecimal usedDays,
    BigDecimal remainingDays) {}
