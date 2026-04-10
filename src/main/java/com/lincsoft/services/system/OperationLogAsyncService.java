package com.lincsoft.services.system;

import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.mapper.system.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous operation log persistence service.
 *
 * <p>Persists operation logs to the database asynchronously. Executed in the {@code asyncExecutor}
 * thread pool to avoid blocking the main request flow.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogAsyncService {

  private final SysOperationLogMapper sysOperationLogMapper;

  /**
   * Saves operation log asynchronously.
   *
   * <p>Executed in the {@code asyncExecutor} thread pool. If an exception occurs during
   * persistence, it is logged but not rethrown to avoid affecting the main request flow.
   *
   * @param operationLog the operation log entity to persist
   */
  @Async("asyncExecutor")
  public void saveOperationLog(SysOperationLog operationLog) {
    try {
      sysOperationLogMapper.insert(operationLog);
    } catch (Exception e) {
      // Asynchronous processing: log exception only, do not rethrow
      log.error("Failed to save operation log: {}", e.getMessage(), e);
    }
  }
}
