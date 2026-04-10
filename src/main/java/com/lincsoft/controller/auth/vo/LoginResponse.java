package com.lincsoft.controller.auth.vo;

/**
 * Login response VO.
 *
 * <p>Only contains the access token. The refresh token is delivered via HttpOnly cookie.
 *
 * @param accessToken The JWT access token.
 * @author 林创科技
 * @since 2026-04-10
 */
public record LoginResponse(String accessToken) {}
