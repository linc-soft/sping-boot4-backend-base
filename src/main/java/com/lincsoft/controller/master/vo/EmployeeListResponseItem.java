package com.lincsoft.controller.master.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee list item response VO.
 *
 * @param id User ID
 * @param username Username
 * @param nickname Display name
 * @param sex Gender code
 * @param hiredDate Hire date
 * @param status User status
 * @param updateBy Updated by
 * @param updateAt Updated at
 * @param version Version
 * @param totalAnnualDays Total annual leave days
 * @param usedAnnualDays Used annual leave days
 * @param remainAnnualDays Remaining annual leave days
 * @param otherLeaveDays Other leave days used
 * @author lincsoft
 * @since 2026-06-03
 */
public record EmployeeListResponseItem(
    Long id,
    String username,
    String nickname,
    Integer sex,
    LocalDate hiredDate,
    String status,
    String updateBy,
    LocalDateTime updateAt,
    Integer version,
    java.math.BigDecimal totalAnnualDays,
    java.math.BigDecimal usedAnnualDays,
    java.math.BigDecimal remainAnnualDays,
    java.math.BigDecimal otherLeaveDays) {}
