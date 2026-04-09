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

  /** Maximum text length to store in the error log */
  int MAX_TEXT_LENGTH = 4000;

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

  /** CSRF cookie name */
  String CSRF_COOKIE_NAME = "csrfToken";

  /** CSRF header name */
  String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

  /** Redis key prefix for JWT token blacklist */
  String REDIS_TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

  /** Cache name for UserDetails entries, used by {@code @Cacheable} in UserService. */
  String REDIS_USER_DETAILS_PREFIX = "user:details";
}
