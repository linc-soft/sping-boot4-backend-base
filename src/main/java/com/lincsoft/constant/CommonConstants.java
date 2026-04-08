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
}
