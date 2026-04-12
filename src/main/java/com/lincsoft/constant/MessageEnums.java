package com.lincsoft.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Message enumeration class, used to unify the codes and messages of returned results.
 *
 * <p>Success: 200
 *
 * <p>Fail: 500
 *
 * <p>HTTP Status: Error codes and error messages are added with reference to the <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP Status Codes</a>.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@AllArgsConstructor
public enum MessageEnums {
  SUCCESS(200, null),
  FAIL(500, null),
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  FORBIDDEN(403, "Forbidden"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  USER_NOT_FOUND(100001, "User Not Found"),
  USER_INACTIVE(100002, "User Inactive"),
  TOKEN_REVOKED(100003, "Token Revoked"),
  INVALID_CREDENTIALS(100004, "Invalid Credentials"),
  INVALID_REFRESH_TOKEN(100005, "Invalid Refresh Token"),
  IP_BLOCKED(100006, "IP Blocked"),
  ACCOUNT_LOCKED(100007, "Account Locked"),
  RATE_LIMITED(429, "Too Many Requests");

  @Getter private final int code;
  @Getter private final String message;
}
