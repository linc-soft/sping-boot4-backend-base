package com.lincsoft.controller.log.vo;

import com.lincsoft.common.PageRequest;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Error log page query request.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ErrorLogPageRequest extends PageRequest {
  /** Trace ID (exact match) */
  private String traceId;

  /**
   * Keyword for fuzzy search across multiple fields.
   *
   * <p>Searches in: exception_file, exception_class, exception_method, exception_message,
   * root_cause_message, stack_trace (OR condition).
   */
  private String keyword;

  /** Username (partial match) */
  private String username;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;

  public Set<String> allowedSortColumns() {
    return Set.of(
        "id",
        "trace_id",
        "exception_file",
        "exception_class",
        "exception_method",
        "exception_line",
        "request_method",
        "request_url",
        "client_ip",
        "username",
        "create_time");
  }
}
