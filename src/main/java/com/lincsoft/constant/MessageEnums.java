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

  // Export task
  SYS_EXPORT_TASK_NOT_FOUND(999_404, "error.sys.export_task_not_found"),
  SYS_EXPORT_TASK_FORBIDDEN(999_403, "error.sys.export_task_forbidden"),
  SYS_EXPORT_MAX_RECORDS_EXCEEDED(999_413, "error.sys.export_max_records_exceeded"),
  SYS_EXPORT_FILE_NOT_FOUND(999_404, "error.sys.export_file_not_found"),
  SYS_EXPORT_FILE_EXPIRED(999_410, "error.sys.export_file_expired"),

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

  // ========== 103: OA - Department page ==========
  DEPT_CIRCULAR_DEPENDENCY(103_400, "error.dept.circular_dependency"),
  DEPT_PARENT_NOT_FOUND(103_401, "error.dept.parent_not_found"),
  DEPT_NOT_FOUND(103_404, "error.dept.not_found"),
  DEPT_CODE_EXISTS(103_409, "error.dept.code_exists"),
  DEPT_OPTIMISTIC_LOCK_FAILED(103_412, "error.dept.optimistic_lock_failed"),
  DEPT_HAS_CHILDREN(103_423, "error.dept.has_children"),
  DEPT_HAS_USERS(103_424, "error.dept.has_users"),

  // ========== 104: OA - Position page ==========
  POSITION_NOT_FOUND(104_404, "error.position.not_found"),
  POSITION_CODE_EXISTS(104_409, "error.position.code_exists"),
  POSITION_OPTIMISTIC_LOCK_FAILED(104_412, "error.position.optimistic_lock_failed"),
  POSITION_HAS_USERS(104_424, "error.position.has_users"),

  // ========== 105: System - Resource page ==========
  RESOURCE_NOT_FOUND(105_404, "error.resource.not_found"),
  RESOURCE_CODE_EXISTS(105_409, "error.resource.code_exists"),
  RESOURCE_OPTIMISTIC_LOCK_FAILED(105_412, "error.resource.optimistic_lock_failed"),
  RESOURCE_INVALID_ROLE_CODE(105_400, "error.resource.invalid_role_code"),
  RESOURCE_INVALID_PARENT(105_401, "error.resource.invalid_parent"),
  RESOURCE_DIRECTORY_ROLE_CODE_MUST_BE_NULL(
      105_402, "error.resource.directory_role_code_must_null"),
  RESOURCE_HAS_CHILDREN(105_423, "error.resource.has_children");

  @Getter private final int code;

  @Getter private final String messageKey;
}
