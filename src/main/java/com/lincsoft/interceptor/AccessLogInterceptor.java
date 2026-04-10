package com.lincsoft.interceptor;

import com.lincsoft.constant.CommonConstants;
import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.services.system.AccessLogAsyncService;
import com.lincsoft.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Access log interceptor.
 *
 * <p>{@link HandlerInterceptor} implementation that records access logs for all HTTP requests.
 * Records the request start time in {@code preHandle}, and collects request/response information in
 * {@code afterCompletion} for asynchronous persistence via {@link AccessLogAsyncService}.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Records request method, URL, parameters, headers, and body
 *   <li>Records response status code, headers, and body
 *   <li>Records client IP (proxy-aware), User-Agent, and operating user
 *   <li>Records processing time (milliseconds) and traceId
 *   <li>Masks sensitive data (Authorization header, password fields)
 *   <li>Reads response body via {@link ContentCachingResponseWrapper}
 * </ul>
 *
 * @author LINC Technology
 * @since 2026-04-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

  /** Request attribute key for storing the request start time */
  private static final String START_TIME_ATTR = "accessLog_startTime";

  /** Access log asynchronous persistence service */
  private final AccessLogAsyncService accessLogAsyncService;

  /**
   * Pre-request processing: records the start time.
   *
   * <p>Sets the current time (milliseconds) as a request attribute, used to calculate the
   * processing time in {@code afterCompletion}.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param handler the handler object
   * @return always {@code true} (continues the request chain)
   */
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    // Save request start time to request attribute
    request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
    return true;
  }

  /**
   * Post-request processing: builds and asynchronously persists the access log.
   *
   * <p>Collects all request/response information to build a {@link SysAccessLog} object, applies
   * sensitive data masking, and persists asynchronously via {@link AccessLogAsyncService}. Wrapped
   * in try-catch to ensure exceptions do not affect the request response.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param handler the handler object
   * @param ex the exception thrown during request processing, if any
   */
  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex) {
    try {
      // Calculate processing time
      long duration = calculateDuration(request);

      // Build SysAccessLog object
      SysAccessLog accessLog = new SysAccessLog();

      // Retrieve traceId from MDC
      accessLog.setTraceId(MDC.get(CommonConstants.MDC_TRACE_ID_KEY));

      // Set request information
      accessLog.setRequestMethod(request.getMethod());
      accessLog.setRequestUrl(request.getRequestURI());
      accessLog.setRequestParams(request.getQueryString());
      accessLog.setRequestHeaders(LogUtil.buildRequestHeaders(request));
      accessLog.setRequestBody(LogUtil.extractRequestBody(request));

      // Set response information
      accessLog.setResponseStatus(response.getStatus());
      accessLog.setResponseHeaders(buildResponseHeaders(response));
      accessLog.setResponseBody(extractResponseBody(response));

      // Set client information
      accessLog.setClientIp(LogUtil.getClientIp(request));
      accessLog.setUserAgent(request.getHeader("User-Agent"));

      // Retrieve operating user from MDC
      accessLog.setUsername(MDC.get(CommonConstants.MDC_CURRENT_USER_KEY));

      // Set processing time and creation timestamp
      accessLog.setDuration(duration);
      accessLog.setCreateTime(LocalDateTime.now());

      // Asynchronously persist access log
      accessLogAsyncService.saveAccessLog(accessLog);

    } catch (Exception e) {
      // Ensure access log recording failure does not affect request response
      log.error("Error occurred while recording access log: {}", e.getMessage(), e);
    }
  }

  // ========== Private Helper Methods ==========

  /**
   * Calculates the request processing time.
   *
   * @param request the HTTP request
   * @return processing time in milliseconds, or 0 if start time cannot be retrieved
   */
  private long calculateDuration(HttpServletRequest request) {
    Object startTimeObj = request.getAttribute(START_TIME_ATTR);
    if (startTimeObj instanceof Long startTime) {
      return System.currentTimeMillis() - startTime;
    }
    return 0L;
  }

  /**
   * Builds response headers as a JSON string.
   *
   * @param response the HTTP response
   * @return JSON string of response headers
   */
  private String buildResponseHeaders(HttpServletResponse response) {
    try {
      StringJoiner joiner = new StringJoiner(",", "{", "}");
      Collection<String> headerNames = response.getHeaderNames();
      if (headerNames != null) {
        for (String name : headerNames) {
          String value = response.getHeader(name);
          joiner.add("\"" + LogUtil.escapeJson(name) + "\":\"" + LogUtil.escapeJson(value) + "\"");
        }
      }
      return joiner.toString();
    } catch (Exception e) {
      log.warn("Failed to convert response headers to JSON: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Extracts the response body.
   *
   * <p>The body can only be read if the response is a {@link ContentCachingResponseWrapper}.
   * Truncates if it exceeds the maximum length.
   *
   * @param response the HTTP response
   * @return response body string, or null if it cannot be retrieved
   */
  private String extractResponseBody(HttpServletResponse response) {
    if (response instanceof ContentCachingResponseWrapper wrapper) {
      byte[] content = wrapper.getContentAsByteArray();
      if (content.length > 0) {
        String body = new String(content, StandardCharsets.UTF_8);
        return LogUtil.truncate(body, CommonConstants.MAX_TEXT_LENGTH);
      }
    }
    return null;
  }
}
