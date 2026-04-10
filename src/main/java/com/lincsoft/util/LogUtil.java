package com.lincsoft.util;

import com.lincsoft.constant.CommonConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Common log processing utility class.
 *
 * <p>Aggregates utility methods shared across log processing modules ({@code AccessLogInterceptor},
 * {@code GlobalExceptionHandler}, {@code OperationLogAspect}).
 *
 * <p>Provided features:
 *
 * <ul>
 *   <li>{@link #getClientIp(HttpServletRequest)}: Retrieves client IP with proxy support
 *   <li>{@link #getClientIp()}: Retrieves client IP via {@link RequestContextHolder}
 *   <li>{@link #isValidIp(String)}: Validates IP address
 *   <li>{@link #truncate(String, int)}: Truncates string to specified maximum length
 *   <li>{@link #sanitizeBody(String)}: Masks sensitive information in JSON body
 *   <li>{@link #escapeJson(String)}: Escapes JSON special characters
 *   <li>{@link #buildRequestHeaders(HttpServletRequest)}: Builds request headers as JSON string
 *       (with sensitive header masking)
 *   <li>{@link #extractRequestBody(HttpServletRequest)}: Extracts request body
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Slf4j
public final class LogUtil {

  /** Set of sensitive header names (case-insensitive lookup via TreeSet) */
  private static final Set<String> SENSITIVE_HEADERS;

  static {
    TreeSet<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    headers.add(CommonConstants.HEADER_AUTHORIZATION);
    headers.add(CommonConstants.HEADER_COOKIE);
    headers.add(CommonConstants.HEADER_SET_COOKIE);
    SENSITIVE_HEADERS = headers;
  }

  /** Regular expression pattern to detect sensitive fields (built from CommonConstants) */
  private static final Pattern SENSITIVE_FIELD_PATTERN =
      Pattern.compile(
          "(\"(?:" + CommonConstants.SENSITIVE_FIELD_NAMES + ")\"\\s*:\\s*\")([^\"]*)(\")",
          Pattern.CASE_INSENSITIVE);

  private LogUtil() {
    throw new AssertionError("Utility class: instantiation not allowed");
  }

  /**
   * Retrieves the client IP address from the HTTP request.
   *
   * <p>Supports requests via proxy/load balancer by retrieving the IP address in the following
   * priority order:
   *
   * <ol>
   *   <li>X-Forwarded-For header (first IP in comma-separated list)
   *   <li>X-Real-IP header
   *   <li>Proxy-Client-IP header
   *   <li>WL-Proxy-Client-IP header (for WebLogic)
   *   <li>Fallback: {@code request.getRemoteAddr()}
   * </ol>
   *
   * @param request the HTTP request
   * @return the client IP address
   */
  public static String getClientIp(HttpServletRequest request) {
    // Check X-Forwarded-For header (for proxy requests, the first IP is the original client IP)
    String ip = request.getHeader("X-Forwarded-For");
    if (isValidIp(ip)) {
      return ip.split(",")[0].trim();
    }

    // Check X-Real-IP header
    ip = request.getHeader("X-Real-IP");
    if (isValidIp(ip)) {
      return ip.trim();
    }

    // Check Proxy-Client-IP header
    ip = request.getHeader("Proxy-Client-IP");
    if (isValidIp(ip)) {
      return ip.trim();
    }

    // Check WL-Proxy-Client-IP header (for WebLogic)
    ip = request.getHeader("WL-Proxy-Client-IP");
    if (isValidIp(ip)) {
      return ip.trim();
    }

    // Fallback: use remote address
    return request.getRemoteAddr();
  }

  /**
   * Retrieves the HTTP request from {@link RequestContextHolder} and returns the client IP address.
   *
   * <p>Used when {@link HttpServletRequest} cannot be obtained directly in AOP aspects or request
   * contexts. Returns {@code null} if the request context does not exist (e.g., scheduled tasks).
   *
   * @return the client IP address, or {@code null} if no request context is available
   */
  public static String getClientIp() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        // No request context (e.g., scheduled tasks)
        return null;
      }
      return getClientIp(attributes.getRequest());
    } catch (Exception e) {
      // Return null on IP retrieval failure to avoid affecting the main flow
      log.warn("Failed to retrieve client IP address: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Determines whether an IP address string is valid.
   *
   * <p>Returns {@code true} if all of the following conditions are met:
   *
   * <ul>
   *   <li>Not {@code null}
   *   <li>Not empty
   *   <li>Not "unknown" (case-insensitive)
   * </ul>
   *
   * @param ip the IP address string
   * @return {@code true} if valid, {@code false} if invalid
   */
  public static boolean isValidIp(String ip) {
    return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
  }

  /**
   * Truncates a string to the specified maximum length.
   *
   * <p>Returns {@code null} if the input is {@code null}. Returns the string as-is if its length is
   * less than or equal to {@code maxLen}. If it exceeds, truncates to {@code maxLen} characters and
   * appends a truncation suffix.
   *
   * @param str the target string
   * @param maxLen the maximum length
   * @return the truncated string, or {@code null} if the input is {@code null}
   */
  public static String truncate(String str, int maxLen) {
    if (str == null) {
      return null;
    }
    if (str.length() <= maxLen) {
      return str;
    }
    return str.substring(0, maxLen) + CommonConstants.TRUNCATE_SUFFIX;
  }

  /**
   * Masks sensitive fields in a JSON body.
   *
   * <p>Replaces JSON values matching the following field names with "{@value
   * CommonConstants#MASK_VALUE}": password, passwd, pwd, secret, token, credential
   * (case-insensitive).
   *
   * @param body the JSON body string
   * @return the masked body string, or the input as-is if it is {@code null} or empty
   */
  public static String sanitizeBody(String body) {
    if (body == null || body.isEmpty()) {
      return body;
    }
    return SENSITIVE_FIELD_PATTERN
        .matcher(body)
        .replaceAll("$1" + CommonConstants.MASK_VALUE + "$3");
  }

  /**
   * Escapes special characters in a JSON string.
   *
   * <p>Characters to escape:
   *
   * <ul>
   *   <li>{@code "} → {@code \"}
   *   <li>{@code \} → {@code \\}
   *   <li>newline → {@code \n}
   *   <li>carriage return → {@code \r}
   *   <li>tab → {@code \t}
   * </ul>
   *
   * @param value the string to escape
   * @return the escaped string, or an empty string if the input is {@code null}
   */
  public static String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      switch (ch) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> sb.append(ch);
      }
    }
    return sb.toString();
  }

  /**
   * Extracts the request body from {@link ContentCachingRequestWrapper} and masks sensitive
   * information.
   *
   * <p>The body can only be read if the request is wrapped with {@link
   * ContentCachingRequestWrapper}. The extracted body is masked for sensitive fields via {@link
   * #sanitizeBody(String)}, truncated to {@value CommonConstants#MAX_TEXT_LENGTH} characters, and
   * returned. Returns {@code null} otherwise.
   *
   * @param request the HTTP request
   * @return the request body string with sensitive information masked and truncated (UTF-8
   *     decoded), or {@code null} if it cannot be retrieved
   */
  public static String extractRequestBody(HttpServletRequest request) {
    if (request instanceof ContentCachingRequestWrapper wrapper) {
      byte[] content = wrapper.getContentAsByteArray();
      if (content.length > 0) {
        // UTF-8 decode, mask sensitive fields, and truncate to maximum length
        return truncate(
            sanitizeBody(new String(content, StandardCharsets.UTF_8)),
            CommonConstants.MAX_TEXT_LENGTH);
      }
    }
    return null;
  }

  /**
   * Builds request headers as a JSON string.
   *
   * <p>Sensitive headers (Authorization, Cookie, Set-Cookie) are masked with "{@value
   * CommonConstants#MASK_VALUE}". Header names and values are JSON-escaped.
   *
   * @param request the HTTP request
   * @return the JSON string of request headers, or {@code null} if building fails
   */
  public static String buildRequestHeaders(HttpServletRequest request) {
    try {
      StringJoiner joiner = new StringJoiner(",", "{", "}");
      Enumeration<String> headerNames = request.getHeaderNames();
      if (headerNames != null) {
        while (headerNames.hasMoreElements()) {
          String name = headerNames.nextElement();
          String value;
          if (SENSITIVE_HEADERS.contains(name)) {
            // Mask sensitive header values
            value = CommonConstants.MASK_VALUE;
          } else {
            value = request.getHeader(name);
          }
          joiner.add("\"" + escapeJson(name) + "\":\"" + escapeJson(value) + "\"");
        }
      }
      return joiner.toString();
    } catch (Exception e) {
      log.warn("Failed to convert request headers to JSON: {}", e.getMessage());
      return null;
    }
  }
}
