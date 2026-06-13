package com.lincsoft.controller.log.vo;

import com.lincsoft.common.PageRequest;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * SQL log page query request.
 *
 * @author 林创科技
 * @since 2026-06-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SqlLogPageRequest extends PageRequest {
  /** Trace ID (exact match) */
  private String traceId;

  /** Username (partial match) */
  private String username;

  /** SQL type filter (SELECT/INSERT/UPDATE/DELETE) */
  private String sqlType;

  /** API path (partial match) */
  private String requestUrl;

  /** Minimum duration in milliseconds */
  private Long minDuration;

  /** Maximum duration in milliseconds */
  private Long maxDuration;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;
}
