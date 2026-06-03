package com.lincsoft.controller.master.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Employee response VO (combined mst_user + mst_employee data).
 *
 * @param id User ID
 * @param username Username
 * @param email Email
 * @param status User status
 * @param roleIds Assigned role IDs
 * @param version Version
 * @param nickname Display name
 * @param mobile Mobile phone
 * @param sex Gender code
 * @param hiredDate Hire date
 * @param remark Remark
 * @author lincsoft
 * @since 2026-06-03
 */
public record EmployeeResponse(
    Long id,
    String username,
    String email,
    String status,
    List<Long> roleIds,
    Integer version,
    String nickname,
    String mobile,
    Integer sex,
    LocalDate hiredDate,
    String remark,
    BigDecimal totalAnnualDays,
    BigDecimal usedAnnualDays,
    BigDecimal remainAnnualDays,
    BigDecimal otherLeaveDays) {}
