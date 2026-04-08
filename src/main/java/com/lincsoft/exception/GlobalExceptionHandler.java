package com.lincsoft.exception;

import com.lincsoft.common.Result;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.services.system.ErrorLogAsyncService;
import com.lincsoft.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 *
 * <p>all exceptions and return an appropriate {@link Result} response.Set error codes and messages
 * according to each exception type, and save error logs asynchronously through {@link
 * ErrorLogAsyncService}.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Slf4j
@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
  private final ErrorLogAsyncService errorLogAsyncService;

  /**
   * Handle business exceptions.
   *
   * @param e The business exception to handle.
   * @param request The current HTTP request.
   * @return A result indicating the error.
   */
  @ExceptionHandler(BusinessException.class)
  public Result<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
    // Log the stack trace of the business exception.
    log.error(
        "A business exception has occurred: code={}, message={}", e.getCode(), e.getMessage(), e);
    // Save error logs asynchronously.
    saveErrorLog(e, request);
    // Return a custom error code and message.
    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * Handle validation exceptions.
   *
   * @param e The validation exception to handle.
   * @param request The current HTTP request.
   * @return A result indicating the error.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<Void> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {
    // Combine validation error messages
    String errorMessage =
        e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining("; "));
    // Log the stack trace of the validation exception.
    log.error("Validation exception occurred: {}", errorMessage, e);
    // Save error logs asynchronously.
    saveErrorLog(e, request);
    // Return a bad request error code and message.
    return Result.error(MessageEnums.BAD_REQUEST);
  }

  /**
   * Handle authentication exceptions.
   *
   * @param e The authentication exception to handle.
   * @param request The current HTTP request.
   * @return A result indicating the error.
   */
  @ExceptionHandler(AuthenticationException.class)
  public Result<Void> handleAuthenticationException(
      AuthenticationException e, HttpServletRequest request) {
    // Log the stack trace of the authentication exception.
    log.error("An authentication exception has occurred: {}", e.getMessage(), e);
    // Save error logs asynchronously.
    saveErrorLog(e, request);
    // Return a forbidden error code and message.
    return Result.error(MessageEnums.UNAUTHORIZED);
  }

  /**
   * Handle unexpected exceptions.
   *
   * @param e The exception to handle.
   * @param request The current HTTP request.
   * @return A result indicating the error.
   */
  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e, HttpServletRequest request) {
    // Log the stack trace of the exception.
    log.error("An unexpected exception occurred: {}", e.getMessage(), e);
    // Save error logs asynchronously.
    saveErrorLog(e, request);
    // Return a generic error code and message.
    return Result.error(MessageEnums.INTERNAL_SERVER_ERROR);
  }

  /**
   * Save error logs asynchronously.
   *
   * @param e The exception to save.
   * @param request The current HTTP request.
   */
  private void saveErrorLog(Exception e, HttpServletRequest request) {
    try {
      SysErrorLog errorLog = new SysErrorLog();

      // Get traceId from MDC
      errorLog.setTraceId(MDC.get(CommonConstants.MDC_TRACE_ID_KEY));

      // Set stack trace information (if stack trace exists)
      StackTraceElement[] stackTrace = e.getStackTrace();
      if (stackTrace != null && stackTrace.length > 0) {
        errorLog.setExceptionMethod(stackTrace[0].getMethodName());
        errorLog.setExceptionLine(stackTrace[0].getLineNumber());
        errorLog.setExceptionFile(stackTrace[0].getFileName());
      }

      // Set the exception class name
      errorLog.setExceptionClass(e.getClass().getName());

      // Set the exception message
      errorLog.setExceptionMessage(
          LogUtil.truncate(e.getMessage(), CommonConstants.MAX_TEXT_LENGTH));

      // Set the root cause message
      errorLog.setRootCauseMessage(
          LogUtil.truncate(getRootCauseMessage(e), CommonConstants.MAX_TEXT_LENGTH));

      // Set the stack trace
      errorLog.setStackTrace(
          LogUtil.truncate(getStackTraceAsString(e), CommonConstants.MAX_TEXT_LENGTH));

      // Save error logs asynchronously.
      errorLogAsyncService.saveErrorLog(errorLog);
    } catch (Exception ex) {
      // Exceptions thrown during the error log saving process itself will be logged and will not
      // affect the main flow.
      log.error("An error occurred during the error log saving process", ex);
    }
  }

  /**
   * Get the root cause message of the exception.
   *
   * @param e The exception to get the root cause message for.
   * @return The root cause message.
   */
  private String getRootCauseMessage(Throwable e) {
    Throwable cause = e;
    // Traverse to the root cause exception at the lowest level (with circular reference prevention)
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause.getMessage();
  }

  /**
   * Get the stack trace as a string.
   *
   * @param e The exception to get the stack trace for.
   * @return The stack trace as a string.
   */
  private String getStackTraceAsString(Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
