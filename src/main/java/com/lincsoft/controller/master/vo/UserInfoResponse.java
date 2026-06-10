package com.lincsoft.controller.master.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * User info response VO.
 *
 * @param id User ID
 * @param username Username
 * @param email Email address
 * @param status User status
 * @param realName Real name
 * @param deptId Department ID
 * @param positionId Position ID
 * @param mobile Mobile phone
 * @param gender Gender (0 unknown / 1 male / 2 female)
 * @param birthday Birthday
 * @param roleIds Directly assigned role IDs (never {@code null}; empty when none)
 * @param version Version
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserInfoResponse(
    Long id,
    String username,
    String email,
    String status,
    String realName,
    Long deptId,
    Long positionId,
    String mobile,
    String gender,
    LocalDate birthday,
    List<Long> roleIds,
    Integer version) {}
