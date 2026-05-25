package com.lincsoft.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Message enumeration class, used to uniformly return the status code and message key of the
 * result.
 *
 * <p>Each enumeration item is associated with a message key in the format of {@ code error.
 * {enumeration name lowercase underscore}}.
 *
 * <p>The message text is parsed from a multilingual resource file through the Message Service.
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
  BAD_REQUEST(400, "error.bad_request"),
  UNAUTHORIZED(401, "error.unauthorized"),
  FORBIDDEN(403, "error.forbidden"),
  NOT_FOUND(404, "error.not_found"),
  UNIQUE_CONSTRAINT_VIOLATION(409, "error.unique_constraint_violation"),
  INSERT_FAILED(411, "error.insert_failed"),
  OPTIMISTIC_LOCK_FAILED(412, "error.optimistic_lock_failed"),
  RESOURCE_IS_USED(423, "error.resource_is_used"),
  RATE_LIMITED(429, "error.rate_limited"),
  INTERNAL_SERVER_ERROR(500, "error.internal_server_error"),
  /* System Error Codes 100_001 - 100_999 */
  TOKEN_REVOKED(100_001, "error.token_revoked"),
  INVALID_CREDENTIALS(100_002, "error.invalid_credentials"),
  INVALID_REFRESH_TOKEN(100_003, "error.invalid_refresh_token"),
  IP_BLOCKED(100_004, "error.ip_blocked"),
  /* Master Data Error Codes 200_001 - 200_999 */
  USER_NOT_FOUND(200_001, "error.user_not_found"),
  USER_INACTIVE(200_002, "error.user_inactive"),
  USERNAME_CANNOT_BE_UPDATED(200_003, "error.username_cannot_be_updated"),
  CIRCULAR_DEPENDENCY(200_004, "error.circular_dependency"),
  BASE_ROLE_CANNOT_BE_DELETED(200_005, "error.base_role_cannot_be_deleted");

  @Getter private final int code;

  @Getter private final String messageKey;
}
