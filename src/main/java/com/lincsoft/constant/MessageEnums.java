package com.lincsoft.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Message enumeration class, used to uniformly return the status code and message key of the
 * result.
 *
 * <p>Each enumeration item is associated with a message key for i18n resolution.
 *
 * <p>Code convention:
 *
 * <ul>
 *   <li>200: Success
 *   <li>400~499: Client-side parameter errors (generic, no module prefix)
 *   <li>500~599: Server-side exceptions (generic, no module prefix)
 *   <li>XXX_400~XXX_499: Client-side parameter errors for module XXX
 *   <li>XXX_500~XXX_599: Server-side exceptions for module XXX
 * </ul>
 *
 * <p>Module codes:
 *
 * <ul>
 *   <li>101: Master - User page
 *   <li>102: Master - Role page
 *   <li>999: System module (auth, common, log, etc.)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@AllArgsConstructor
public enum MessageEnums {
  // ========== Generic (400~499: client parameter errors, 500~599: server exceptions) ==========
  SUCCESS(200, null),
  FAIL(500, null),
  BAD_REQUEST(400, "error.bad_request"),
  UNAUTHORIZED(401, "error.unauthorized"),
  FORBIDDEN(403, "error.forbidden"),
  RATE_LIMITED(429, "error.rate_limited"),
  INTERNAL_SERVER_ERROR(500, "error.internal_server_error"),

  // ========== 999: System module (auth, common, log, etc.) ==========
  // 999_404 reserved: system data not found
  SYS_TOKEN_REVOKED(999_401, "error.sys.token_revoked"),
  SYS_INVALID_CREDENTIALS(999_402, "error.sys.invalid_credentials"),
  SYS_INVALID_REFRESH_TOKEN(999_403, "error.sys.invalid_refresh_token"),
  SYS_IP_BLOCKED(999_423, "error.sys.ip_blocked"),
  // 999: Password reset
  SYS_PASSWORD_RESET_TOKEN_INVALID(999_404, "error.sys.password_reset_token_invalid"),
  SYS_CURRENT_PASSWORD_MISMATCH(999_401, "error.sys.current_password_mismatch"),
  SYS_PASSWORD_RESET_EMAIL_SENT(200, "auth.password.reset_email_sent"),
  SYS_PASSWORD_RESET_SUCCESS(200, "auth.password.reset_success"),
  SYS_PASSWORD_CHANGE_SUCCESS(200, "auth.password.change_success"),

  // ========== 101: Master - User page ==========
  // 101_404: user not found
  USER_USERNAME_CANNOT_BE_UPDATED(101_400, "error.user.username_cannot_be_updated"),
  USER_INACTIVE(101_403, "error.user.inactive"),
  USER_NOT_FOUND(101_404, "error.user.not_found"),
  USER_USERNAME_EXISTS(101_409, "error.user.username_exists"),
  USER_EMAIL_EXISTS(101_409, "error.user.email_exists"),
  USER_INSERT_FAILED(101_411, "error.user.insert_failed"),
  USER_OPTIMISTIC_LOCK_FAILED(101_412, "error.user.optimistic_lock_failed"),

  // ========== 102: Master - Role page ==========
  // 102_404: role not found
  ROLE_CIRCULAR_DEPENDENCY(102_400, "error.role.circular_dependency"),
  ROLE_BASE_CANNOT_BE_DELETED(102_403, "error.role.base_cannot_be_deleted"),
  ROLE_NOT_FOUND(102_404, "error.role.not_found"),
  ROLE_INHERITANCE_EXISTS(102_409, "error.role.inheritance_exists"),
  ROLE_OPTIMISTIC_LOCK_FAILED(102_412, "error.role.optimistic_lock_failed"),
  ROLE_IN_USE(102_423, "error.role.in_use"),
  ROLE_HAS_INHERITANCE(102_424, "error.role.has_inheritance");

  @Getter private final int code;

  @Getter private final String messageKey;
}
