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

  /** Status code (business code from response body) */
  private Integer statusCode;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;
}
