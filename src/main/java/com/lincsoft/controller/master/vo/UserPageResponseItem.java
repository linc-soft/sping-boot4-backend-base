package com.lincsoft.controller.master.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * User page item response VO.
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
 * @param updateBy Update user
 * @param updateAt Update time
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserPageResponseItem(
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
    String updateBy,
    LocalDateTime updateAt,
    Integer version) {}
