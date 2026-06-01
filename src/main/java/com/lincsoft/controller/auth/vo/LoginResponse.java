package com.lincsoft.controller.auth.vo;

/**
 * Login response VO.
 *
 * <p>Contains the access token and a flag indicating whether the user must change their password on
 * first login (when status is INACTIVE).
 *
 * @param accessToken The JWT access token.
 * @param requirePasswordChange Whether the user must change their password before accessing other
 *     resources.
 * @author 林创科技
 * @since 2026-04-10
 */
public record LoginResponse(String accessToken, boolean requirePasswordChange) {

  /** Convenience constructor for standard login without forced password change. */
  public LoginResponse(String accessToken) {
    this(accessToken, false);
  }
}
