package com.lincsoft.filter;

import com.lincsoft.common.Result;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.util.LogUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Rate Limiting Filter using Bucket4j token-bucket algorithm.
 *
 * <p>This filter applies per-IP rate limiting to all incoming HTTP requests. Each unique client IP
 * address is assigned a token bucket with a configurable capacity and refill rate. When a client
 * exceeds the allowed request rate, a 429 Too Many Requests response is returned.
 *
 * <p>Configuration is driven by {@link AppProperties.RateLimit}:
 *
 * <ul>
 *   <li>Capacity: Maximum burst size (tokens in the bucket)
 *   <li>Refill tokens and period: Token replenishment rate
 *   <li>Enabled flag: Allows disabling rate limiting entirely
 * </ul>
 *
 * <p>Response headers included:
 *
 * <ul>
 *   <li>{@code X-Rate-Limit-Remaining}: Number of requests remaining in the current window
 *   <li>{@code Retry-After}: Seconds until the next token becomes available (only on 429 responses)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-09
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  /** Application configuration properties for rate limit settings. */
  private final AppProperties appProperties;

  /** Object Mapper for JSON response serialization. */
  private final ObjectMapper objectMapper;

  /** Per-IP token bucket cache. Uses ConcurrentHashMap for thread-safe access. */
  private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

  /**
   * Applies rate limiting to the incoming request based on the client IP address.
   *
   * <p>If rate limiting is disabled via configuration, the request passes through directly. If the
   * client has remaining tokens, the request proceeds and the remaining count is added to the
   * response headers. If the bucket is exhausted, a 429 response is returned with a Retry-After
   * header.
   *
   * @param request the HTTP servlet request
   * @param response the HTTP servlet response
   * @param filterChain the filter chain to continue processing
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Skip rate limiting if disabled
    if (!appProperties.getRateLimit().isEnabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    // Resolve client IP address (proxy-aware, uses X-Forwarded-For etc.)
    String clientIp = LogUtil.getClientIp(request);
    Bucket bucket = bucketCache.computeIfAbsent(clientIp, _ -> createBucket());

    // Try to consume one token
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      // Request allowed: add remaining tokens to response header
      response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      filterChain.doFilter(request, response);
    } else {
      // Request rejected: return 429 Too Many Requests
      long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
      response.setHeader("Retry-After", String.valueOf(waitSeconds));
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json;charset=UTF-8");
      response
          .getWriter()
          .write(objectMapper.writeValueAsString(Result.error(MessageEnums.RATE_LIMITED)));
      log.warn("Rate limit exceeded for IP: {}", clientIp);
    }
  }

  /**
   * Creates a new token bucket with the configured rate limit parameters.
   *
   * @return a new Bucket instance
   */
  private Bucket createBucket() {
    AppProperties.RateLimit config = appProperties.getRateLimit();
    Bandwidth limit =
        Bandwidth.builder()
            .capacity(config.getCapacity())
            .refillGreedy(
                config.getRefillTokens(), Duration.ofSeconds(config.getRefillPeriodSeconds()))
            .build();
    return Bucket.builder().addLimit(limit).build();
  }
}
