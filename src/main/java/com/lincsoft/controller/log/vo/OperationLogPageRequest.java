package com.lincsoft.controller.log.vo;

import com.lincsoft.common.PageRequest;
import com.lincsoft.constant.OperationEnums;
import java.time.LocalDateTime;
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
  private OperationEnums operationType;

  /** Module code (exact match) */
  private String module;

  /** Sub-module code (exact match) */
  private String subModule;

  /** Username (partial match) */
  private String username;

  /** Start time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  /** End time */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endTime;
}
