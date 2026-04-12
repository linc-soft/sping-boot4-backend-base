package com.lincsoft.constant;

/**
 * Common constants interface.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
public interface CommonConstants {
  /** MDC default username */
  String MDC_DEFAULT_USERNAME = "system";

  /** MDC key for current user */
  String MDC_CURRENT_USER_KEY = "CurrentUser";

  /** MDC key for request timestamp */
  String MDC_REQUEST_TIMESTAMP_KEY = "RequestTimestamp";

  /** MDC key for trace ID */
  String MDC_TRACE_ID_KEY = "traceId";

  /**
   * Maximum character length for MySQL TEXT column (65,535 bytes / utf8mb4 max 4 bytes ≈ 16,383)
   */
  int MAX_TEXT_LENGTH = 16000;

  /** Suffix to truncate the text */
  String TRUNCATE_SUFFIX = "...[truncated]";

  /** Authorization header name */
  String AUTHORIZATION_HEADER = "Authorization";

  /** Bearer token prefix */
  String BEARER_PREFIX = "Bearer ";

  /** JWT claim for the authenticated user */
  String JWT_CLAIM_USER_KEY = "AuthenticatedUser";

  /** User status inactive */
  String USER_STATUS_INACTIVE = "0";

  /** User status active */
  String USER_STATUS_ACTIVE = "1";

  /** CSRF cookie name */
  String CSRF_COOKIE_NAME = "csrfToken";

  /** CSRF header name */
  String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

  /** Redis key prefix for JWT token blacklist */
  String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

  /** Redis key prefix for active session (username → refresh token JTI) */
  String REDIS_ACTIVE_SESSION_PREFIX = "token:session:";

  /** Cache name for UserDetails entries, used by {@code @Cacheable} in UserService. */
  String REDIS_USER_DETAILS_PREFIX = "user:details";

  /** Trace ID response header name */
  String HEADER_TRACE_ID = "X-Trace-Id";

  /** Authorization header name */
  String HEADER_AUTHORIZATION = "Authorization";

  /** Content-Type header name */
  String HEADER_CONTENT_TYPE = "Content-Type";

  /** X-CSRF-TOKEN header name */
  String HEADER_X_CSRF_TOKEN = "X-CSRF-TOKEN";

  /** Cookie header name */
  String HEADER_COOKIE = "Cookie";

  /** Set-Cookie header name */
  String HEADER_SET_COOKIE = "Set-Cookie";

  /** Mask replacement string for sensitive data */
  String MASK_VALUE = "******";

  /** Maximum length of the User-Agent field (to match VARCHAR(255) in the DDL) */
  int MAX_USER_AGENT_LENGTH = 255;

  /** Sensitive field names for JSON body masking (pipe-separated for regex alternation) */
  String SENSITIVE_FIELD_NAMES = "password|passwd|pwd|secret|token|credential";

  /** JWT claim key to distinguish token type (access / refresh) */
  String JWT_CLAIM_TOKEN_TYPE_KEY = "tokenType";

  /** Token type value: access */
  String TOKEN_TYPE_ACCESS = "access";

  /** Token type value: refresh */
  String TOKEN_TYPE_REFRESH = "refresh";

  /** Refresh token cookie name */
  String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

  /** Refresh token cookie path (only sent to refresh endpoint) */
  String REFRESH_TOKEN_COOKIE_PATH = "/api/auth/refresh";

  /** Redis key prefix for IP blacklist (auto-blocked IPs) */
  String REDIS_IP_BLOCKED_PREFIX = "ip:blocked:";

  /** Redis key prefix for login failure count per IP */
  String REDIS_LOGIN_FAIL_IP_PREFIX = "login:fail:ip:";

  /** Redis key prefix for login failure count per account */
  String REDIS_LOGIN_FAIL_ACCOUNT_PREFIX = "login:fail:account:";

  /** Redis key prefix for account lock */
  String REDIS_LOGIN_LOCKED_PREFIX = "login:locked:";
}
