package com.lincsoft.controller.auth.vo;

/**
 * Refresh token response VO.
 *
 * <p>Only contains the new access token. The new refresh token is delivered via HttpOnly cookie
 * (Token Rotation).
 *
 * @param accessToken The new JWT access token.
 * @author 林创科技
 * @since 2026-04-10
 */
public record RefreshResponse(String accessToken) {}
