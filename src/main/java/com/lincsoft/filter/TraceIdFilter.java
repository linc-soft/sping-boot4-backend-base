package com.lincsoft.filter;

import com.lincsoft.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Link trace filter for request tracing.
 *
 * <p>Prioritizes the traceId from the incoming request header ({@code X-Trace-Id}) for
 * cross-service tracing. If no traceId is present, generates a new one. Sets the traceId in the MDC
 * context, request attributes, and response headers. After request completion, clears the traceId
 * from MDC to prevent data leakage during thread pool reuse.
 *
 * <p><b>Design Note:</b> This filter intentionally does not use the {@code @Component} annotation.
 * Since Spring Boot automatically registers {@code @Bean} filters with the Servlet container, using
 * {@code @Component} would cause double execution in both SecurityFilterChain and the Servlet
 * container. By instantiating with {@code new} and manually registering via {@code
 * addFilterBefore()}, we ensure it executes exactly once at the correct position in the security
 * filter chain.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
public class TraceIdFilter extends OncePerRequestFilter {

  /**
   * Reads traceId from the request header ({@code X-Trace-Id}) if present, otherwise generates a
   * new one. Sets it in MDC, request attributes, and response headers. Clears MDC in the finally
   * block to prevent data leakage during thread pool reuse.
   *
   * @param request HTTP servlet request
   * @param response HTTP servlet response
   * @param filterChain the filter chain
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Prioritize traceId from upstream request header for cross-service tracing
    String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString().replace("-", "");
    }

    // Set traceId in MDC (automatically attached to log output)
    MDC.put(CommonConstants.MDC_TRACE_ID_KEY, traceId);

    // Set traceId in request attributes (available to downstream components)
    request.setAttribute(CommonConstants.MDC_TRACE_ID_KEY, traceId);

    // Set traceId in response headers (accessible to clients)
    response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);

    try {
      // Continue the filter chain
      filterChain.doFilter(request, response);
    } finally {
      // Clear MDC (prevent data leakage during thread pool reuse)
      MDC.remove(CommonConstants.MDC_TRACE_ID_KEY);
    }
  }
}
