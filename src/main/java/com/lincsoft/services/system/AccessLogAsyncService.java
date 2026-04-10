package com.lincsoft.services.system;

import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.mapper.system.SysAccessLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous access log persistence service.
 *
 * <p>Persists access logs to the database asynchronously. Executed in the {@code asyncExecutor}
 * thread pool to avoid blocking the main request flow.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogAsyncService {

  private final SysAccessLogMapper sysAccessLogMapper;

  /**
   * Saves access log asynchronously.
   *
   * <p>Executed in the {@code asyncExecutor} thread pool. If an exception occurs during
   * persistence, it is logged but not rethrown to avoid affecting the main request flow.
   *
   * @param accessLog the access log entity to persist
   */
  @Async("asyncExecutor")
  public void saveAccessLog(SysAccessLog accessLog) {
    try {
      sysAccessLogMapper.insert(accessLog);
    } catch (Exception e) {
      // Asynchronous processing: log exception only, do not rethrow
      log.error("Failed to save access log: {}", e.getMessage(), e);
    }
  }
}
