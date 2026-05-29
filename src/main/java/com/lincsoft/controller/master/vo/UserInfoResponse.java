package com.lincsoft.controller.master.vo;

import java.util.List;

/**
 * User info response VO.
 *
 * @param id User ID
 * @param username Username
 * @param email Email address
 * @param status User status
 * @param roleIds Directly assigned role IDs (never {@code null}; empty when none)
 * @param version Version
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserInfoResponse(
    Long id, String username, String email, String status, List<Long> roleIds, Integer version) {}
