package com.lincsoft.services.system;

import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.mapper.system.SysErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * System Error Log Async Service
 *
 * <p>Provides asynchronous methods for saving error logs.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogAsyncService {
  private final SysErrorLogMapper sysErrorLogMapper;

  /**
   * Asynchronously saves the error log.
   *
   * <p>Executed asynchronously using the {@code asyncExecutor} thread pool. If an exception occurs
   * during the save operation, it will be logged in the error log and the exception will not be
   * re-thrown (to avoid affecting the main flow due to asynchronous processing).
   *
   * @param errorLog The error log entity to be saved.
   */
  @Async("asyncExecutor")
  public void saveErrorLog(SysErrorLog errorLog) {
    try {
      sysErrorLogMapper.insert(errorLog);
    } catch (Exception e) {
      // Due to asynchronous processing, exceptions will be logged without being thrown.
      log.error("Failed to save the exception log: {}", e.getMessage(), e);
    }
  }
}
