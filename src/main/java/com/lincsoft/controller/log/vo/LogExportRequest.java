package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Log export request — filter-only, no pagination.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
@Data
public class LogExportRequest {

  /** Trace ID (exact match) */
  private String traceId;

  /** Username (partial match) */
  private String username;

  /** HTTP method */
  private String method;

  /** API path (partial match) */
  private String path;

  /**
   * Status code filter. Accepts exact match (e.g. "200") or range patterns: "2XX" = 200-299, "4XX"
   * = 400-499, "5XX" = 500-599.
   */
  private String statusCode;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;
}
