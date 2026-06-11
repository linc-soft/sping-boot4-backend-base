package com.lincsoft.services.system;

import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.entity.system.SysSqlLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Trace Service.
 *
 * <p>Provides trace-related operations for combining access logs, error logs, and operation logs.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceService {
  private final AccessLogService accessLogService;
  private final ErrorLogService errorLogService;
  private final OperationLogService operationLogService;
  private final SqlLogService sqlLogService;

  /**
   * Get access log by trace ID.
   *
   * @param traceId Trace ID
   * @return Access log entity
   */
  public SysAccessLog getAccessLog(String traceId) {
    return accessLogService.getByTraceId(traceId);
  }

  /**
   * Get error log by trace ID.
   *
   * @param traceId Trace ID
   * @return Error log entity, or null if not found
   */
  public SysErrorLog getErrorLog(String traceId) {
    return errorLogService.getByTraceId(traceId);
  }

  /**
   * Get operation logs by trace ID.
   *
   * @param traceId Trace ID
   * @return List of operation log entities
   */
  public List<SysOperationLog> getOperationLogs(String traceId) {
    return operationLogService.getListByTraceId(traceId);
  }

  /**
   * Get SQL logs by trace ID.
   *
   * @param traceId Trace ID
   * @return List of SQL log entities
   */
  public List<SysSqlLog> getSqlLogs(String traceId) {
    return sqlLogService.getListByTraceId(traceId);
  }
}
