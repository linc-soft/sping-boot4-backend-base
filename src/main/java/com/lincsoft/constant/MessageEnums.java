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
  NOT_FOUND(404, "%s Not Found"),
  UNIQUE_CONSTRAINT_VIOLATION(409, "The %s already exists. Please use a different value."),
  OPTIMISTIC_LOCK_FAILED(
      412, "The %s has been modified by another user. Please refresh and try again."),
  RESOURCE_IS_USED(423, "The %s is used by other resources. Please check and try again."),
  RATE_LIMITED(429, "Too Many Requests"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  /* System Error Codes 100_001 - 100_999 */
  TOKEN_REVOKED(100_001, "Token Revoked"),
  INVALID_CREDENTIALS(100_002, "Invalid Credentials"),
  INVALID_REFRESH_TOKEN(100_003, "Invalid Refresh Token"),
  IP_BLOCKED(100_004, "IP Blocked"),
  /* Master Data Error Codes 200_001 - 200_999 */
  USER_NOT_FOUND(200_001, "User Not Found"),
  USER_INACTIVE(200_002, "User Inactive"),
  USERNAME_CANNOT_BE_UPDATED(200_003, "Username cannot be updated"),
  CIRCULAR_DEPENDENCY(200_004, "Circular dependency detected in %s. Please check the hierarchy.");

  @Getter private final int code;
  @Getter private final String message;

  /**
   * Format message with arguments.
   *
   * @param message Message template
   * @param args Arguments to format the message
   * @return Formatted message string
   */
  public static String format(MessageEnums message, Object... args) {
    return String.format(message.message, args);
  }
}
