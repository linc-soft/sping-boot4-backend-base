package com.lincsoft.controller.log.vo;

import com.lincsoft.common.PageRequest;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Access log page query request.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AccessLogPageRequest extends PageRequest {
  /** Trace ID (exact match) */
  private String traceId;

  /** Username (partial match) */
  private String username;

  /** HTTP method */
  private String method;

  /** API path (partial match) */
  private String path;

  /**
   * Status code filter. Accepts exact match (e.g. "200") or range patterns: "4XX" — match status
   * codes 400–499 and module-prefixed codes (XXX_400–XXX_499) "5XX" — match status codes 500–599
   * and module-prefixed codes (XXX_500–XXX_599)
   */
  private String statusCode;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;
}
