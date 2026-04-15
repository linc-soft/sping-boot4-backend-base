package com.lincsoft.services.system;

import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.lincsoft.config.AppProperties;
import com.lincsoft.entity.system.SysAccessLog;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Access log batch persistence service.
 *
 * <p>Buffers access log entries in a {@link ConcurrentLinkedQueue} and flushes them to the database
 * in batches via a scheduled task. This significantly reduces database pressure under high
 * concurrency compared to single-row INSERT per request.
 *
 * <p>Flush is triggered by:
 *
 * <ul>
 *   <li>Scheduled fixed-rate task (configurable via {@code app.access-log.flush-interval-ms})
 *   <li>Application shutdown ({@link PreDestroy} callback ensures remaining logs are persisted)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogAsyncService {

  private final AppProperties appProperties;

  /** In-memory buffer for pending access log entries. Thread-safe, lock-free. */
  private final ConcurrentLinkedQueue<SysAccessLog> buffer = new ConcurrentLinkedQueue<>();

  /**
   * Enqueues an access log entry into the in-memory buffer.
   *
   * <p>This method is called from {@code AccessLogInterceptor.afterCompletion()} and returns almost
   * instantly since it only adds to a lock-free queue.
   *
   * @param accessLog the access log entity to buffer
   */
  public void saveAccessLog(SysAccessLog accessLog) {
    buffer.offer(accessLog);
  }

  /**
   * Scheduled task that drains the buffer and performs batch INSERT.
   *
   * <p>Runs at a fixed rate defined by {@code app.access-log.flush-interval-ms}. Each invocation
   * loops to drain multiple batches (up to {@code app.access-log.max-batches-per-flush}) so that
   * traffic spikes do not cause unbounded buffer growth.
   *
   * <p>Uses MyBatis-Plus {@link Db#saveBatch(java.util.Collection)} for efficient batch insertion.
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
}
