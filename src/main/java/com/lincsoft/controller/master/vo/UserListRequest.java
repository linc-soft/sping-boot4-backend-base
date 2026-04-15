package com.lincsoft.controller.master.vo;

/**
 * User list query request VO.
 *
 * @param username Username (partial match)
 * @param status User status
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserListRequest(String username, String status) {}
