package com.lincsoft.util;

import static com.lincsoft.constant.CommonConstants.REDIS_IP_BLOCKED_PREFIX;

import com.lincsoft.config.AppProperties;
import com.lincsoft.services.auth.LoginProtectionService;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

/**
 * IP Address Checker.
 *
 * <p>Centralizes all IP-level access control checks, including whitelist matching and blacklist
 * (Redis-based block) lookups. Exposes static methods for use across filters and services without
 * requiring injection.
 *
 * <p>Whitelist entries are pre-compiled from {@link AppProperties.RateLimit#getWhiteList()} at
 * startup. Supports individual IPs (e.g., {@code 203.0.113.50}) and CIDR notation (e.g., {@code
 * 10.0.0.0/8}) via Spring Security's {@link IpAddressMatcher}.
 *
 * <p>Blacklist checks query the Redis key {@code ip:blocked:{ip}}, which is written by {@link
 * LoginProtectionService} when an IP exceeds the login failure threshold.
 *
 * @author 林创科技
 * @since 2026-04-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpChecker {

  /** Application configuration injected by Spring, used only during initialization. */
  private final AppProperties appProperties;

  /** Redis template injected by Spring, used only during initialization. */
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Static list of pre-compiled whitelist matchers, initialized once at startup.
   *
   * <p>Never modified after initialization — safe for concurrent reads.
   */
  private static List<IpAddressMatcher> matchers = List.of();

  /**
   * Static reference to the Redis template, initialized once at startup.
   *
   * <p>Held statically so that {@link #isBlocked(String)} can be called without injection.
   */
  private static RedisTemplate<String, Object> redis;

  /**
   * Initializes static fields from injected Spring beans.
   *
   * <p>Called once by Spring after construction. Compiles whitelist entries into {@link
   * IpAddressMatcher} instances and stores the Redis template reference for static use.
   */
  @PostConstruct
  private void init() {
    List<String> whiteList = appProperties.getRateLimit().getWhiteList();
    matchers =
        whiteList.stream()
            .map(
                entry -> {
                  IpAddressMatcher matcher = new IpAddressMatcher(entry.trim());
                  log.info("IP whitelist entry compiled: {}", entry.trim());
                  return matcher;
                })
            .toList();
    log.info("IP whitelist initialized with {} entries", matchers.size());
    redis = redisTemplate;
  }

  /**
   * Checks whether the given IP address matches any entry in the whitelist.
   *
   * <p>Defensively catches {@link IllegalArgumentException} thrown by {@link
   * IpAddressMatcher#matches(String)} when the input is not a valid IP address (e.g., a forged or
   * malformed {@code X-Forwarded-For} header value). In such cases the IP is treated as
   * non-whitelisted to avoid bypassing rate-limit or login-protection logic.
   *
   * @param ip the client IP address to check
   * @return {@code true} if the IP matches any whitelist entry, {@code false} otherwise
   */
  public static boolean isWhitelisted(String ip) {
    if (ip == null || ip.isBlank()) {
      return false;
    }
    for (IpAddressMatcher matcher : matchers) {
      try {
        if (matcher.matches(ip)) {
          return true;
        }
      } catch (IllegalArgumentException e) {
        // Malformed or forged IP string — treat as non-whitelisted and log at debug level
        // to avoid flooding logs with attacker-controlled input.
        log.debug("Skipping whitelist check for invalid IP string: {}", ip);
        return false;
      }
    }
    return false;
  }

  /**
   * Checks whether the given IP address is currently blacklisted in Redis.
   *
   * <p>The blacklist key ({@code ip:blocked:{ip}}) is written by {@link LoginProtectionService}
   * when an IP exceeds the configured login failure threshold, and expires automatically after the
   * configured block duration.
   *
   * @param ip the client IP address to check
   * @return {@code true} if the IP is blacklisted, {@code false} otherwise
   */
  public static boolean isBlocked(String ip) {
    if (ip == null || ip.isBlank()) {
      return false;
    }
    return Boolean.TRUE.equals(redis.hasKey(REDIS_IP_BLOCKED_PREFIX + ip));
  }
}
