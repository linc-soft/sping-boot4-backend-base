package com.lincsoft.controller.master.vo;

import java.time.LocalDateTime;

/**
 * Role list item response VO.
 *
 * @param id Role ID
 * @param roleName Role name
 * @param roleCode Role code
 * @param description Role description
 * @param updateBy Update user
 * @param updateAt Update time
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleListResponseItem(
    Long id,
    String roleName,
    String roleCode,
    String description,
    String updateBy,
    LocalDateTime updateAt) {}
