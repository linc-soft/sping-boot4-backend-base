package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.lincsoft.config.AppProperties;
import com.lincsoft.controller.log.vo.AccessLogPageRequest;
import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysAccessLogMapper;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Access Log Service.
 *
 * <p>Provides query operations and async batch persistence for access logs.
 *
 * <p>Write operations use an in-memory buffer with scheduled batch flush to reduce database
 * pressure under high concurrency. Query operations are synchronous.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogService {
  private final SysAccessLogMapper accessLogMapper;
  private final AppProperties appProperties;

  /** In-memory buffer for pending access log entries. Thread-safe, lock-free. */
  private final ConcurrentLinkedQueue<SysAccessLog> buffer = new ConcurrentLinkedQueue<>();

  // ==================== Query Operations ====================

  /**
   * Get access log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of access log entities
   */
  public IPage<SysAccessLog> getPage(AccessLogPageRequest request) {
    QueryWrapper<SysAccessLog> queryWrapper = buildQueryWrapper(request);
    request.applySorting(
        queryWrapper,
        Set.of(
            "id",
            "trace_id",
            "request_method",
            "request_url",
            "response_status",
            "client_ip",
            "user_agent",
            "username",
            "duration",
            "create_time"),
        "create_time");
    return accessLogMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get access log by ID.
   *
   * @param id Access log ID
   * @return Access log entity
   * @throws BusinessException if the log is not found
   */
  public SysAccessLog getById(Long id) {
    SysAccessLog entity = accessLogMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("Access log not found with id: " + id);
    }
    return entity;
  }

  /**
   * Get access log by trace ID.
   *
   * @param traceId Trace ID
   * @return Access log entity
   * @throws BusinessException if the log is not found
   */
  public SysAccessLog getByTraceId(String traceId) {
    QueryWrapper<SysAccessLog> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("trace_id", traceId);
    return accessLogMapper.selectOne(queryWrapper);
  }

  /**
   * Export access logs to CSV format.
   *
   * @param request Query conditions
   * @return CSV data as byte array
   */
  public byte[] export(AccessLogPageRequest request) {
    QueryWrapper<SysAccessLog> queryWrapper = buildQueryWrapper(request);
    request.applySorting(
        queryWrapper,
        Set.of(
            "id",
            "trace_id",
            "request_method",
            "request_url",
            "response_status",
            "client_ip",
            "user_agent",
            "username",
            "duration",
            "create_time"),
        "create_time");
    // Limit export to 10000 records to avoid memory issues
    queryWrapper.last("LIMIT 10000");
    List<SysAccessLog> logs = accessLogMapper.selectList(queryWrapper);

    StringBuilder csv = new StringBuilder();
    csv.append("ID,TraceID,Username,Method,Path,StatusCode,Duration,ClientIP,CreatedAt\n");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    for (SysAccessLog logEntity : logs) {
      csv.append(escapeCsv(String.valueOf(logEntity.getId()))).append(",");
      csv.append(escapeCsv(logEntity.getTraceId())).append(",");
      csv.append(escapeCsv(logEntity.getUsername())).append(",");
      csv.append(escapeCsv(logEntity.getRequestMethod())).append(",");
      csv.append(escapeCsv(logEntity.getRequestUrl())).append(",");
      csv.append(escapeCsv(String.valueOf(logEntity.getResponseStatus()))).append(",");
      csv.append(escapeCsv(String.valueOf(logEntity.getDuration()))).append(",");
      csv.append(escapeCsv(logEntity.getClientIp())).append(",");
      csv.append(
              escapeCsv(
                  logEntity.getCreateTime() != null
                      ? logEntity.getCreateTime().format(formatter)
                      : ""))
          .append("\n");
    }

    // Add BOM for UTF-8 to ensure Excel compatibility
    byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
    byte[] result = new byte[bom.length + csvBytes.length];
    System.arraycopy(bom, 0, result, 0, bom.length);
    System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);
    return result;
  }

  // ==================== Write Operations ====================

  /**
   * Enqueues an access log entry into the in-memory buffer.
   *
   * <p>This method is called from {@code AccessLogInterceptor.afterCompletion()} and returns almost
   * instantly since it only adds to a lock-free queue.
   *
   * @param accessLog the access log entity to buffer
   */
  public void save(SysAccessLog accessLog) {
    buffer.offer(accessLog);
  }

  /**
   * Scheduled task that drains the buffer and performs batch INSERT.
   *
   * <p>Runs at a fixed rate defined by {@code app.access-log.flush-interval-ms}. Each invocation
   * loops to drain multiple batches (up to {@code app.access-log.max-batches-per-flush}) so that
   * traffic spikes do not cause unbounded buffer growth.
   */
  @Scheduled(fixedRateString = "${app.access-log.flush-interval-ms:5000}")
  public void flushLogs() {
    int batchSize = appProperties.getAccessLog().getBatchSize();
    int maxBatches = appProperties.getAccessLog().getMaxBatchesPerFlush();
    int totalFlushed = 0;

    for (int i = 0; i < maxBatches; i++) {
      List<SysAccessLog> batch = drainBuffer(batchSize);
      if (batch.isEmpty()) {
        break;
      }
      try {
        Db.saveBatch(batch);
        totalFlushed += batch.size();
      } catch (Exception e) {
        log.error(
            "Failed to batch insert access logs ({} entries): {}", batch.size(), e.getMessage(), e);
        break;
      }
    }

    if (totalFlushed > 0) {
      log.debug("Flushed {} access log(s) to database", totalFlushed);
    }

    if (!buffer.isEmpty()) {
      log.warn(
          "Access log buffer still has {} pending entries after flush (max batches reached)",
          buffer.size());
    }
  }

  /**
   * Flushes all remaining buffered logs on application shutdown.
   *
   * <p>Ensures no log entries are lost during graceful shutdown. Drains the entire buffer
   * regardless of batch size limits.
   */
  @PreDestroy
  public void onShutdown() {
    log.info("Application shutting down — flushing remaining access logs...");
    List<SysAccessLog> remaining = new ArrayList<>();
    SysAccessLog entry;
    while ((entry = buffer.poll()) != null) {
      remaining.add(entry);
    }
    if (remaining.isEmpty()) {
      log.info("No pending access logs to flush");
      return;
    }
    try {
      Db.saveBatch(remaining);
      log.info("Flushed {} remaining access log(s) on shutdown", remaining.size());
    } catch (Exception e) {
      log.error(
          "Failed to flush access logs on shutdown ({} entries lost): {}",
          remaining.size(),
          e.getMessage(),
          e);
    }
  }

  /**
   * Returns the current number of buffered (unflushed) log entries.
   *
   * <p>Useful for monitoring and health checks.
   *
   * @return number of pending entries in the buffer
   */
  public int getBufferSize() {
    return buffer.size();
  }

  // ==================== Private Methods ====================

  /**
   * Build query wrapper from request.
   *
   * @param request Query request
   * @return QueryWrapper
   */
  private QueryWrapper<SysAccessLog> buildQueryWrapper(AccessLogPageRequest request) {
    QueryWrapper<SysAccessLog> queryWrapper = new QueryWrapper<>();

    if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
      queryWrapper.eq("trace_id", request.getTraceId());
    }

    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      queryWrapper.like("username", request.getUsername());
    }

    if (request.getMethod() != null && !request.getMethod().isBlank()) {
      queryWrapper.eq("request_method", request.getMethod());
    }

    if (request.getPath() != null && !request.getPath().isBlank()) {
      queryWrapper.like("request_url", request.getPath());
    }

    if (request.getStatusCode() != null && !request.getStatusCode().isBlank()) {
      String sc = request.getStatusCode().toUpperCase();
      if ("2XX".equals(sc)) {
        // Match plain 200–299 and module-prefixed XXX_200–XXX_299
        queryWrapper.apply("response_status % 1000 >= 200 AND response_status % 1000 < 300");
      } else if ("4XX".equals(sc)) {
        // Match plain 400–499 and module-prefixed XXX_400–XXX_499
        queryWrapper.apply("response_status % 1000 >= 400 AND response_status % 1000 < 500");
      } else if ("5XX".equals(sc)) {
        // Match plain 500–599 and module-prefixed XXX_500–XXX_599
        queryWrapper.apply("response_status % 1000 >= 500 AND response_status % 1000 < 600");
      } else {
        try {
          queryWrapper.eq("response_status", Integer.parseInt(sc));
        } catch (NumberFormatException ignored) {
          // invalid status code — skip filter
        }
      }
    }

    if (request.getStartTime() != null) {
      queryWrapper.ge("create_time", request.getStartTime());
    }

    if (request.getEndTime() != null) {
      queryWrapper.le("create_time", request.getEndTime());
    }

    return queryWrapper;
  }

  /**
   * Drains up to {@code limit} entries from the buffer into a list.
   *
   * @param limit maximum number of entries to drain
   * @return list of drained entries (maybe empty, never null)
   */
  private List<SysAccessLog> drainBuffer(int limit) {
    List<SysAccessLog> batch = new ArrayList<>(limit);
    SysAccessLog entry;
    while (batch.size() < limit && (entry = buffer.poll()) != null) {
      batch.add(entry);
    }
    return batch;
  }

  /**
   * Escape CSV special characters.
   *
   * @param value Field value
   * @return Escaped value
   */
  private String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
