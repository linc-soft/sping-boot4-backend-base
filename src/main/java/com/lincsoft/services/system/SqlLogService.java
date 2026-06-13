package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.controller.log.vo.SqlLogPageRequest;
import com.lincsoft.entity.system.SysSqlLog;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysSqlLogMapper;
import com.lincsoft.util.LogUtil;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * SQL Log Service.
 *
 * <p>Provides query operations and async batch persistence for SQL logs.
 *
 * <p>Write operations use an in-memory buffer with scheduled batch flush to reduce database
 * pressure under high concurrency. Query operations are synchronous.
 *
 * @author 林创科技
 * @since 2026-06-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlLogService {
  private final SysSqlLogMapper sqlLogMapper;
  private final AppProperties appProperties;

  /** In-memory buffer for pending SQL log entries. Thread-safe, lock-free. */
  private final ConcurrentLinkedQueue<SysSqlLog> buffer = new ConcurrentLinkedQueue<>();

  // ==================== Query Operations ====================

  /**
   * Get SQL log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of SQL log entities
   */
  public IPage<SysSqlLog> getPage(SqlLogPageRequest request) {
    QueryWrapper<SysSqlLog> queryWrapper = buildQueryWrapper(request);
    request.applySorting(
        queryWrapper,
        Set.of(
            "id",
            "trace_id",
            "sql_type",
            "request_url",
            "request_method",
            "duration",
            "username",
            "client_ip",
            "row_count",
            "create_time"),
        "create_time");
    return sqlLogMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get SQL log by ID.
   *
   * @param id SQL log ID
   * @return SQL log entity
   * @throws BusinessException if the log is not found
   */
  public SysSqlLog getById(Long id) {
    SysSqlLog entity = sqlLogMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("SQL log not found with id: " + id);
    }
    return entity;
  }

  /**
   * Get SQL logs by trace ID.
   *
   * @param traceId Trace ID
   * @return SQL log entity list
   */
  public List<SysSqlLog> getListByTraceId(String traceId) {
    QueryWrapper<SysSqlLog> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("trace_id", traceId).orderByDesc("create_time");
    return sqlLogMapper.selectList(queryWrapper);
  }

  // ==================== Write Operations ====================

  /**
   * Enqueues a SQL log entry into the in-memory buffer.
   *
   * <p>This method is called from SQL logging components and returns almost instantly since it only
   * adds to a lock-free queue.
   *
   * @param sqlLog the SQL log entity to buffer
   */
  public void save(SysSqlLog sqlLog) {
    if (sqlLog.getSqlText() != null
        && sqlLog.getSqlText().length() > CommonConstants.MAX_TEXT_LENGTH) {
      sqlLog.setSqlText(LogUtil.truncate(sqlLog.getSqlText(), CommonConstants.MAX_TEXT_LENGTH));
    }
    buffer.offer(sqlLog);
  }

  /**
   * Scheduled task that drains the buffer and performs batch INSERT.
   *
   * <p>Runs at a fixed rate defined by {@code app.sql-log.flush-interval-ms}. Each invocation loops
   * to drain multiple batches (up to {@code app.sql-log.max-batches-per-flush}) so that traffic
   * spikes do not cause unbounded buffer growth.
   */
  @Scheduled(fixedRateString = "${app.sql-log.flush-interval-ms:3000}")
  public void flushLogs() {
    int batchSize = appProperties.getSqlLog().getBatchSize();
    int maxBatches = appProperties.getSqlLog().getMaxBatchesPerFlush();
    int totalFlushed = 0;

    for (int i = 0; i < maxBatches; i++) {
      List<SysSqlLog> batch = drainBuffer(batchSize);
      if (batch.isEmpty()) {
        break;
      }
      try {
        Db.saveBatch(batch);
        totalFlushed += batch.size();
      } catch (Exception e) {
        log.error(
            "Failed to batch insert SQL logs ({} entries): {}", batch.size(), e.getMessage(), e);
        break;
      }
    }

    if (totalFlushed > 0) {
      log.debug("Flushed {} SQL log(s) to database", totalFlushed);
    }

    if (!buffer.isEmpty()) {
      log.warn(
          "SQL log buffer still has {} pending entries after flush (max batches reached)",
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
    log.info("Application shutting down — flushing remaining SQL logs...");
    List<SysSqlLog> remaining = new ArrayList<>();
    SysSqlLog entry;
    while ((entry = buffer.poll()) != null) {
      remaining.add(entry);
    }
    if (remaining.isEmpty()) {
      log.info("No pending SQL logs to flush");
      return;
    }
    try {
      Db.saveBatch(remaining);
      log.info("Flushed {} remaining SQL log(s) on shutdown", remaining.size());
    } catch (Exception e) {
      log.error(
          "Failed to flush SQL logs on shutdown ({} entries lost): {}",
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
  private QueryWrapper<SysSqlLog> buildQueryWrapper(SqlLogPageRequest request) {
    QueryWrapper<SysSqlLog> queryWrapper = new QueryWrapper<>();

    if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
      queryWrapper.eq("trace_id", request.getTraceId());
    }

    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      queryWrapper.like("username", request.getUsername());
    }

    if (request.getSqlType() != null && !request.getSqlType().isBlank()) {
      queryWrapper.eq("sql_type", request.getSqlType());
    }

    if (request.getRequestUrl() != null && !request.getRequestUrl().isBlank()) {
      queryWrapper.like("request_url", request.getRequestUrl());
    }

    if (request.getMinDuration() != null) {
      queryWrapper.ge("duration", request.getMinDuration());
    }

    if (request.getMaxDuration() != null) {
      queryWrapper.le("duration", request.getMaxDuration());
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
  private List<SysSqlLog> drainBuffer(int limit) {
    List<SysSqlLog> batch = new ArrayList<>(limit);
    SysSqlLog entry;
    while (batch.size() < limit && (entry = buffer.poll()) != null) {
      batch.add(entry);
    }
    return batch;
  }
}
