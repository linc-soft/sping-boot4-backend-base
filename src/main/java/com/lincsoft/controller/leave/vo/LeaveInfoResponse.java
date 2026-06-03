package com.lincsoft.controller.leave.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Leave info response VO - annual leave details for the current user.
 *
 * @param annualEffectiveDate1 First effective date (before anniversary)
 * @param annualLeaveDays1 Days before anniversary
 * @param annualEffectiveDate2 Second effective date (after anniversary)
 * @param annualLeaveDays2 Days after anniversary
 * @param leaveInfos List of leave type usage
 * @author lincsoft
 * @since 2026-06-03
 */
public record LeaveInfoResponse(
    String annualEffectiveDate1,
    BigDecimal annualLeaveDays1,
    String annualEffectiveDate2,
    BigDecimal annualLeaveDays2,
    List<LeaveInfoItem> leaveInfos) {

  /** Leave type usage item */
  public record LeaveInfoItem(Integer leaveType, BigDecimal leaveDays) {}
}
