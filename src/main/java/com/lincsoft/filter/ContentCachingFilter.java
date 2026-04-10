package com.lincsoft.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Content caching filter.
 *
 * <p>Wraps {@link HttpServletRequest} with {@link ContentCachingRequestWrapper} and {@link
 * HttpServletResponse} with {@link ContentCachingResponseWrapper} to enable multiple reads of
 * request and response bodies.
 *
 * <p>This filter is required for components that need to read request and response bodies (e.g.,
 * logging interceptors). Standard Servlet request/response streams can only be read once, but using
 * caching wrappers allows body retrieval even after the filter chain completes.
 *
 * <p><b>Important:</b> After the filter chain completes, {@link
 * ContentCachingResponseWrapper#copyBodyToResponse()} must be called to copy the cached response
 * body to the actual response. Failure to do so will result in an empty response being sent to the
 * client.
 *
 * <p><b>Design Note:</b> This filter intentionally does NOT use the {@code @Component} annotation.
 * Spring Boot automatically registers {@code @Bean} filters with the Servlet container. Using
 * {@code @Component} would cause double execution—both in the SecurityFilterChain and the Servlet
 * container. By instantiating with {@code new} and manually registering via {@code
 * addFilterBefore()}, it executes exactly once at the correct position within the security filter
 * chain.
 *
 * @author LincSoft
 * @since 2026-04-10
 */
public class ContentCachingFilter extends OncePerRequestFilter {

  /** Maximum size for request body cache in bytes. Default: 10KB */
  private static final int DEFAULT_CONTENT_CACHE_LIMIT = 10240;

  /**
   * Wraps request/response with caching wrappers and executes the filter chain.
   *
   * <p>Processing flow:
   *
   * <ol>
   *   <li>Wrap request with {@link ContentCachingRequestWrapper} if not already wrapped
   *   <li>Wrap response with {@link ContentCachingResponseWrapper} if not already wrapped
   *   <li>Continue filter chain with wrapped request/response
   *   <li>After filter chain completes, copy cached response body to actual response
   * </ol>
   *
   * @param request HTTP request
   * @param response HTTP response
   * @param filterChain filter chain
   * @throws ServletException if a servlet exception occurs
   * @throws IOException if an I/O exception occurs
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Check if request is already wrapped with ContentCachingRequestWrapper
    ContentCachingRequestWrapper wrappedRequest =
        (request instanceof ContentCachingRequestWrapper cachingRequest)
            ? cachingRequest
            : new ContentCachingRequestWrapper(request, DEFAULT_CONTENT_CACHE_LIMIT);

    // Check if response is already wrapped with ContentCachingResponseWrapper
    // Track whether this filter performed the wrapping to prevent duplicate copyBodyToResponse
    // calls
    boolean wrappedByThis = !(response instanceof ContentCachingResponseWrapper);
    ContentCachingResponseWrapper wrappedResponse =
        wrappedByThis
            ? new ContentCachingResponseWrapper(response)
            : (ContentCachingResponseWrapper) response;

    try {
      // Continue filter chain with wrapped request/response
      filterChain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      // Copy cached response body to actual response only if this filter performed the wrapping
      // If already wrapped externally, copyBodyToResponse is the external wrapper's responsibility
      if (wrappedByThis) {
        wrappedResponse.copyBodyToResponse();
      }
    }
  }
}
