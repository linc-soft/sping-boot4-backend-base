package com.lincsoft.controller.master.vo;

/**
 * User info response VO.
 *
 * @param id User ID
 * @param username Username
 * @param status User status
 * @param version Version
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserInfoResponse(Long id, String username, String status, Integer version) {}
