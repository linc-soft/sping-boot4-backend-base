package com.lincsoft.controller.master.vo;

import java.time.LocalDateTime;

/**
 * User page item response VO.
 *
 * @param id User ID
 * @param username Username
 * @param status User status
 * @param updateBy Update user
 * @param updateAt Update time
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserPageResponseItem(
    Long id, String username, String status, String updateBy, LocalDateTime updateAt) {}
