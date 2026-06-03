package com.lincsoft.dto.leave;

import java.math.BigDecimal;

/**
 * Annual leave calculation intermediate result.
 *
 * @param userId User ID
 * @param year Calendar year
 * @param lastRemaining Carry-over from previous year
 * @param preEffective Days before anniversary date
 * @param postEffective Days after anniversary date
 * @param effectiveDate Anniversary date
 * @param totalDays Total available days for the year
 * @author lincsoft
 * @since 2026-06-03
 */
public record AnnualLeaveCalculation(
    Long userId,
    int year,
    BigDecimal lastRemaining,
    BigDecimal preEffective,
    BigDecimal postEffective,
    java.time.LocalDate effectiveDate,
    BigDecimal totalDays) {}
