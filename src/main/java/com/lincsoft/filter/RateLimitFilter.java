package com.lincsoft.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lincsoft.common.Result;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.util.IpChecker;
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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Rate Limiting and IP Access Control Filter.
 *
 * <p>This filter provides two layers of IP-based request protection, evaluated in order:
 *
 * <ol>
 *   <li><b>Whitelist check:</b> Whitelisted IPs (configured via {@code app.rate-limit.white-list})
 *       bypass rate limiting entirely and proceed directly to the next filter.
 *   <li><b>Rate limiting:</b> All other IPs are subject to per-IP token-bucket rate limiting using
 *       the Bucket4j algorithm.
 * </ol>
 *
 * <p>IP blacklist enforcement is handled separately by {@link IpBlacklistFilter}, which runs before
 * this filter and is always active regardless of the {@code enabled} flag.
 *
 * <p>Configuration is driven by {@link AppProperties.RateLimit}:
 *
 * <ul>
 *   <li>White list: IP addresses/CIDR ranges that bypass rate limiting
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
public class RateLimitFilter extends OncePerRequestFilter {

  /** Application configuration properties for rate limit settings. */
  private final AppProperties appProperties;

  /** Object Mapper for JSON response serialization. */
  private final ObjectMapper objectMapper;

  /**
   * Per-IP token bucket cache. Uses Caffeine cache with expireAfterAccess to automatically evict
   * inactive entries and prevent memory leaks from accumulating buckets for stale IPs.
   */
  private final Cache<String, Bucket> bucketCache;

  /**
   * Creates a new RateLimitFilter with the given dependencies.
   *
   * @param appProperties application configuration properties
   * @param objectMapper object mapper for JSON serialization
   */
  public RateLimitFilter(AppProperties appProperties, ObjectMapper objectMapper) {
    this.appProperties = appProperties;
    this.objectMapper = objectMapper;
    this.bucketCache =
        Caffeine.newBuilder()
            .expireAfterAccess(
                Duration.ofMinutes(appProperties.getRateLimit().getExpireAfterAccessMinutes()))
            .build();
  }

  /**
   * Applies IP access control and rate limiting to the incoming request.
   *
   * <p>Processing order:
   *
   * <ol>
   *   <li>If rate limiting is disabled, pass through directly.
   *   <li>Resolve client IP address (proxy-aware).
   *   <li>If IP is whitelisted → pass through (skip rate limiting).
   *   <li>Otherwise → apply token-bucket rate limiting.
   * </ol>
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

    // 1. Whitelist: bypass rate limiting entirely
    if (IpChecker.isWhitelisted(clientIp)) {
      filterChain.doFilter(request, response);
      return;
    }

    // 2. Rate limiting: token-bucket algorithm
    Bucket bucket = bucketCache.get(clientIp, _ -> createBucket());

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
