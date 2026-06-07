package com.lincsoft.controller.oa.vo;

import java.time.LocalDate;

/**
 * Employee info response VO.
 *
 * @param id Employee ID
 * @param userId Linked login account ID
 * @param employeeNo Employee number
 * @param realName Real name
 * @param deptId Department ID
 * @param positionId Position ID
 * @param managerId Direct supervisor employee ID
 * @param mobile Mobile phone
 * @param email Email address
 * @param gender Gender (0 unknown / 1 male / 2 female)
 * @param idCardNo ID card number
 * @param birthday Date of birth
 * @param hireDate Hire date
 * @param status Employment status (0 left / 1 active / 2 on leave)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record EmployeeInfoResponse(
    Long id,
    Long userId,
    String employeeNo,
    String realName,
    Long deptId,
    Long positionId,
    Long managerId,
    String mobile,
    String email,
    String gender,
    String idCardNo,
    LocalDate birthday,
    LocalDate hireDate,
    String status,
    Integer version) {}
