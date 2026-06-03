package com.lincsoft.controller.log.vo;

import com.lincsoft.common.PageRequest;
import com.lincsoft.constant.OperationType;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Operation log page query request.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OperationLogPageRequest extends PageRequest {
  /** Trace ID (exact match) */
  private String traceId;

  /** Operation type */
  private OperationType operationType;

  /** Module (partial match) */
  private String module;

  /** Sub module (partial match) */
  private String subModule;

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
        "module",
        "sub_module",
        "operation_type",
        "description",
        "duration",
        "request_method",
        "request_url",
        "client_ip",
        "username",
        "create_time");
  }
}
