package com.lincsoft.controller.leave.vo;

import java.math.BigDecimal;

/**
 * Annual leave remaining days response VO.
 *
 * @param remainDays Remaining annual leave days
 * @author lincsoft
 * @since 2026-06-03
 */
public record AnnualRemainResponse(BigDecimal remainDays) {}
