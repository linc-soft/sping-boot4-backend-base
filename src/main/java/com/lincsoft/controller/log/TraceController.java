package com.lincsoft.controller.log;

import com.lincsoft.controller.log.vo.SqlLogDetailResponse;
import com.lincsoft.controller.log.vo.TraceDetailResponse;
import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.mapstruct.AccessLogMapper;
import com.lincsoft.mapstruct.ErrorLogMapper;
import com.lincsoft.mapstruct.OperationLogQueryMapper;
import com.lincsoft.mapstruct.SqlLogMapper;
import com.lincsoft.services.system.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Trace Controller.
 *
 * <p>Provides endpoints for trace-related operations.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Tag(name = "Trace", description = "Trace management API")
@RestController
@RequestMapping("/api/logs/trace")
@RequiredArgsConstructor
public class TraceController {

  private final TraceService traceService;
  private final AccessLogMapper accessLogMapper;
  private final ErrorLogMapper errorLogMapper;
  private final OperationLogQueryMapper operationLogMapper;
  private final SqlLogMapper sqlLogMapper;

  /**
   * Get trace detail by trace ID.
   *
   * <p>Combines access log, error log (if exists), and operation logs into a single response.
   *
   * @param traceId Trace ID
   * @return Trace detail response
   */
  @Operation(
      summary = "Get trace detail",
      description = "Retrieve complete trace information by trace ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_READ.roleCode)")
  @GetMapping("/{traceId}")
  public TraceDetailResponse getDetail(
      @Parameter(description = "Trace ID") @PathVariable String traceId) {
    SysAccessLog accessLog = traceService.getAccessLog(traceId);
    SysErrorLog errorLog = traceService.getErrorLog(traceId);
    List<SysOperationLog> operationLogs = traceService.getOperationLogs(traceId);
    List<SqlLogDetailResponse> sqlLogs =
        sqlLogMapper.toDetailResponseList(traceService.getSqlLogs(traceId));

    return new TraceDetailResponse(
        accessLog != null ? accessLogMapper.toDetailResponse(accessLog) : null,
        errorLog != null ? errorLogMapper.toDetailResponse(errorLog) : null,
        operationLogMapper.toDetailResponseList(operationLogs),
        sqlLogs);
  }
}
