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
  // auth
  SYS_TOKEN_REVOKED(999_401, "error.sys.token_revoked"),
  SYS_INVALID_CREDENTIALS(999_402, "error.sys.invalid_credentials"),
  SYS_INVALID_REFRESH_TOKEN(999_403, "error.sys.invalid_refresh_token"),
  SYS_IP_BLOCKED(999_423, "error.sys.ip_blocked"),
  // Password reset
  SYS_PASSWORD_RESET_TOKEN_INVALID(999_404, "error.sys.password_reset_token_invalid"),
  SYS_CURRENT_PASSWORD_MISMATCH(999_401, "error.sys.current_password_mismatch"),
  SYS_PASSWORD_RESET_EMAIL_SENT(200, "auth.password.reset_email_sent"),
  SYS_PASSWORD_RESET_SUCCESS(200, "auth.password.reset_success"),
  SYS_PASSWORD_CHANGE_SUCCESS(200, "auth.password.change_success"),
  SYS_FORCE_PASSWORD_CHANGE_REQUIRED(999_422, "error.sys.force_password_change_required"),
  SYS_FORCE_PASSWORD_CHANGE_SUCCESS(200, "auth.force_password_change_success"),

  // File Upload
  SYS_FILE_EMPTY(999_410, "error.sys.file_empty"),
  SYS_FILE_TOO_LARGE(999_411, "error.sys.file_too_large"),
  SYS_FILE_EXTENSION_NOT_ALLOWED(999_412, "error.sys.file_extension_not_allowed"),
  SYS_FILE_NOT_FOUND(999_413, "error.sys.file_not_found"),
  SYS_FILE_UPLOAD_FAILED(999_500, "error.sys.file_upload_failed"),
  SYS_FILE_MD5_MISMATCH(999_501, "error.sys.file_md5_mismatch"),

  // ========== 101: Master - User page ==========
  USER_USERNAME_CANNOT_BE_UPDATED(101_400, "error.user.username_cannot_be_updated"),
  USER_EMAIL_REQUIRED(101_401, "error.user.email_required"),
  USER_INACTIVE(101_403, "error.user.inactive"),
  USER_NOT_FOUND(101_404, "error.user.not_found"),
  USER_USERNAME_EXISTS(101_409, "error.user.username_exists"),
  USER_EMAIL_EXISTS(101_409, "error.user.email_exists"),
  USER_INSERT_FAILED(101_411, "error.user.insert_failed"),
  USER_OPTIMISTIC_LOCK_FAILED(101_412, "error.user.optimistic_lock_failed"),

  // ========== 102: Master - Role page ==========
  ROLE_CIRCULAR_DEPENDENCY(102_400, "error.role.circular_dependency"),
  ROLE_BASE_CANNOT_BE_DELETED(102_403, "error.role.base_cannot_be_deleted"),
  ROLE_NOT_FOUND(102_404, "error.role.not_found"),
  ROLE_INHERITANCE_EXISTS(102_409, "error.role.inheritance_exists"),
  ROLE_OPTIMISTIC_LOCK_FAILED(102_412, "error.role.optimistic_lock_failed"),
  ROLE_IN_USE(102_423, "error.role.in_use"),
  ROLE_HAS_INHERITANCE(102_424, "error.role.has_inheritance"),

  // ========== 103: Master - Employee page ==========
  EMPLOYEE_NOT_FOUND(103_404, "error.employee.not_found"),

  // ========== 201: Leave ==========
  LEAVE_DATE_START_AFTER_END(201_400, "error.leave.date_start_after_end"),
  LEAVE_DATE_NOT_SAME_YEAR(201_401, "error.leave.date_not_same_year"),
  LEAVE_OVERLAPPING_DATE(201_402, "error.leave.overlapping_date"),
  LEAVE_ANNUAL_HAS_UNAPPROVED(201_403, "error.leave.annual_has_unapproved"),
  LEAVE_ANNUAL_EXCEED_BALANCE(201_404, "error.leave.annual_exceed_balance"),
  LEAVE_CANNOT_APPROVE_SELF(201_405, "error.leave.cannot_approve_self"),
  LEAVE_NOT_FOUND(201_408, "error.leave.not_found"),
  LEAVE_REASON_REQUIRED(201_406, "error.leave.reason_required"),
  LEAVE_INVALID_DURATION(201_407, "error.leave.invalid_duration"),
  ANNUAL_LEAVE_EXISTS(201_409, "error.annual_leave.exists"),
  ANNUAL_LEAVE_NOT_FOUND(201_410, "error.annual_leave.not_found");

  @Getter private final int code;

  @Getter private final String messageKey;
}
