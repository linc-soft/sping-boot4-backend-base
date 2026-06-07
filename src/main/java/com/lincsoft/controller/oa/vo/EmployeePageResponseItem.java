package com.lincsoft.controller.oa.vo;

import java.time.LocalDate;

/**
 * Employee page item response VO.
 *
 * @param id Employee ID
 * @param userId Linked login account ID
 * @param employeeNo Employee number
 * @param realName Real name
 * @param deptId Department ID
 * @param positionId Position ID
 * @param mobile Mobile phone
 * @param email Email address
 * @param hireDate Hire date
 * @param status Employment status (0 left / 1 active / 2 on leave)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record EmployeePageResponseItem(
    Long id,
    Long userId,
    String employeeNo,
    String realName,
    Long deptId,
    Long positionId,
    String mobile,
    String email,
    LocalDate hireDate,
    String status,
    Integer version) {}
