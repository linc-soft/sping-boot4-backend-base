package com.lincsoft.config;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Asynchronous processing configuration class.
 *
 * <p>Enables Spring's {@code @Async} annotation and defines the thread pool used for asynchronous
 * log storage. Used for asynchronous writing of access logs, error logs, and operation logs,
 * ensuring that the main request flow is not blocked.
 *
 * <p>Implements {@link AsyncConfigurer} to provide a global handler for uncaught exceptions
 * occurring in asynchronous methods.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@AllArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

  private final AppProperties appProperties;

  /**
   * Defines the thread pool executor for asynchronous log storage.
   *
   * <p>Referenced by {@code @Async("asyncExecutor")} annotation. Adopts CallerRunsPolicy to ensure
   * that when the queue is full, tasks are not discarded but executed in the caller thread.
   *
   * <p>Thread pool parameters are externalized via {@code app.async.*} properties, allowing
   * environment-specific tuning.
   *
   * <p>Enables graceful shutdown to wait for unprocessed tasks in the queue to complete when the
   * application stops.
   *
   * @return configured {@link Executor} instance
   */
  @Bean(name = "asyncExecutor")
  public Executor asyncExecutor() {
    AppProperties.Async asyncProps = appProperties.getAsync();
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // Core pool size: number of threads that are always running
    executor.setCorePoolSize(asyncProps.getCorePoolSize());
    // Maximum pool size: maximum number of threads under increased load
    executor.setMaxPoolSize(asyncProps.getMaxPoolSize());
    // Queue capacity: maximum number of waiting tasks
    executor.setQueueCapacity(asyncProps.getQueueCapacity());
    // Thread name prefix: for thread identification in log output
    executor.setThreadNamePrefix(asyncProps.getThreadNamePrefix());
    // Rejection policy: execute in caller thread when queue is full (prevents task discard)
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // Graceful shutdown: wait for queued tasks to complete on shutdown
    executor.setWaitForTasksToCompleteOnShutdown(asyncProps.isWaitForTasksToCompleteOnShutdown());
    // Shutdown wait time in seconds
    executor.setAwaitTerminationSeconds(asyncProps.getAwaitTerminationSeconds());
    // MDC context propagation: copy traceId, username etc. from caller thread to async thread
    executor.setTaskDecorator(
        runnable -> {
          Map<String, String> contextMap = MDC.getCopyOfContextMap();
          return () -> {
            try {
              if (contextMap != null) {
                MDC.setContextMap(contextMap);
              }
              runnable.run();
            } finally {
              MDC.clear();
            }
          };
        });
    // Initialize the executor
    executor.initialize();
    return executor;
  }

  /**
   * Provides an uncaught exception handler for asynchronous methods.
   *
   * <p>Exceptions occurring in {@code void} return type {@code @Async} methods do not propagate to
   * the caller, so this handler logs them.
   *
   * @return asynchronous exception handler
   */
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) ->
        log.error(
            "Uncaught exception occurred in async method. Method: {}, Args: {}, Error: {}",
            method.getName(),
            params,
            ex.getMessage(),
            ex);
  }
}
