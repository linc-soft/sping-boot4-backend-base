package com.lincsoft.services.auth;

import static com.lincsoft.constant.CommonConstants.*;

import com.lincsoft.config.AppProperties;
import com.lincsoft.filter.PreAuthenticationChecks;
import com.lincsoft.util.IpChecker;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.stereotype.Service;

/**
 * Login Protection Service.
 *
 * <p>Provides brute-force attack protection through two dimensions:
 *
 * <ul>
 *   <li><b>Account dimension (all IPs):</b> Tracks per-account login failures. When failures exceed
 *       the configured threshold (X), the account is locked with an incrementally increasing
 *       duration: {@code (failureCount - X) * N minutes}, capped at M hours.
 *   <li><b>IP dimension (non-whitelisted IPs only):</b> Tracks per-IP login failures. When failures
 *       exceed the configured threshold, the IP is automatically added to the Redis blacklist,
 *       blocking all requests (not just login) for a configurable duration.
 * </ul>
 *
 * <p>Redis key design:
 *
 * <ul>
 *   <li>{@code login:fail:account:{username}} — account failure counter
 *   <li>{@code login:locked:{username}} — account lock marker (value = failure count)
 *   <li>{@code login:fail:ip:{ip}} — IP failure counter (non-whitelisted only)
 *   <li>{@code ip:blocked:{ip}} — IP blacklist marker (non-whitelisted only)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final AppProperties appProperties;

  /**
   * Checks whether the given account is currently locked in Redis.
   *
   * <p>This method is called by {@link PreAuthenticationChecks} during the {@link
   * DaoAuthenticationProvider} authentication flow, before password validation. If the account is
   * locked, {@link PreAuthenticationChecks} throws {@link LockedException}, which is caught
   * alongside {@link BadCredentialsException} in {@link AuthService} and mapped to the same {@code
   * INVALID_CREDENTIALS} error — preventing username enumeration via distinct error codes or
   * response timing.
   *
   * @param username the username to check
   * @return {@code true} if the account is currently locked, {@code false} otherwise
   */
  public boolean isAccountLocked(String username) {
    String lockKey = REDIS_LOGIN_LOCKED_PREFIX + username;
    Boolean locked = redisTemplate.hasKey(lockKey);
    if (Boolean.TRUE.equals(locked)) {
      Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.MINUTES);
      log.warn("Account is locked: account={}, remaining={} minutes", username, ttl);
      return true;
    }
    return false;
  }

  /**
   * Records a failed login attempt and applies protection measures.
   *
   * <p>This method performs two operations:
   *
   * <ol>
   *   <li><b>Account dimension:</b> Increments the account failure counter. If the counter exceeds
   *       the threshold (X), the account is locked with duration = {@code (failureCount - X) * N
   *       minutes}, capped at M hours.
   *   <li><b>IP dimension (non-whitelisted only):</b> Increments the IP failure counter. If the
   *       counter exceeds the IP threshold, the IP is added to the Redis blacklist.
   * </ol>
   *
   * @param username the username that failed authentication
   * @param clientIp the client IP address of the failed attempt
   */
  public void recordFailure(String username, String clientIp) {
    AppProperties.RateLimit config = appProperties.getRateLimit();

    // === Account dimension (all IPs) ===
    handleAccountFailure(username, config);

    // === IP dimension (non-whitelisted IPs only) ===
    if (!IpChecker.isWhitelisted(clientIp)) {
      handleIpFailure(clientIp, config);
    }
  }

  /**
   * Reduces the account failure counter on successful login and clears the lock marker.
   *
   * <p>Instead of clearing the failure counter entirely, it is decremented by {@code
   * accountSuccessDecrement} (floored at 0). This gives legitimate users leniency after a
   * successful login while preventing attackers from using a known-good password to fully reset the
   * counter and restart the incremental lock escalation from scratch.
   *
   * <p>The IP failure counter is cleared entirely on success, as IP-level tracking uses a sliding
   * window model where a successful login indicates the IP is legitimate.
   *
   * @param username the username that successfully authenticated
   * @param clientIp the client IP address of the successful attempt
   */
  public void recordSuccess(String username, String clientIp) {
    AppProperties.RateLimit config = appProperties.getRateLimit();
    String failKey = REDIS_LOGIN_FAIL_ACCOUNT_PREFIX + username;

    // Decrement account failure counter rather than clearing it entirely.
    // This preserves the incremental lock escalation history while giving legitimate users
    // some leniency — one accidental mistype after a successful login won't trigger a lock.
    Object raw = redisTemplate.opsForValue().get(failKey);
    if (raw != null) {
      long current = Long.parseLong(raw.toString());
      long decremented = current - config.getAccountSuccessDecrement();
      if (decremented <= 0) {
        // Counter is effectively zero — remove the key entirely to keep Redis clean
        redisTemplate.delete(failKey);
      } else {
        redisTemplate.opsForValue().set(failKey, decremented);
      }
    }

    // Always clear the lock marker so the user can log in
    redisTemplate.delete(REDIS_LOGIN_LOCKED_PREFIX + username);

    // Clear IP failure counter (only for non-whitelisted IPs)
    if (!IpChecker.isWhitelisted(clientIp)) {
      redisTemplate.delete(REDIS_LOGIN_FAIL_IP_PREFIX + clientIp);
    }

    log.debug(
        "Login success recorded: username={}, ip={}, counterDecremented={}",
        username,
        clientIp,
        config.getAccountSuccessDecrement());
  }

  /**
   * Handles account-level failure counting and locking.
   *
   * <p>Increments the failure counter (no TTL — the counter persists until decremented by a
   * successful login). If the counter exceeds the threshold, calculates the lock duration as {@code
   * (failures - threshold) * stepMinutes}, capped at {@code maxHours}, and sets the lock key in
   * Redis with the calculated TTL.
   *
   * @param username the username that failed authentication
   * @param config the rate limit configuration
   */
  private void handleAccountFailure(String username, AppProperties.RateLimit config) {
    String failKey = REDIS_LOGIN_FAIL_ACCOUNT_PREFIX + username;

    // Increment failure counter
    Long failures = redisTemplate.opsForValue().increment(failKey);
    if (failures == null) {
      return;
    }

    // No TTL is set on the account failure counter intentionally.
    // The counter must persist until cleared by a successful login (via decrement).
    // Setting a TTL would allow attackers to wait out the window and reset the counter for free,
    // completely defeating the incremental lock escalation design.

    log.info("Account login failure recorded: username={}, failures={}", username, failures);

    // Check if threshold exceeded
    int threshold = config.getAccountMaxFailures();
    if (failures > threshold) {
      // Calculate lock duration: (failures - threshold) * stepMinutes, capped at maxHours
      long excessFailures = failures - threshold;
      long lockMinutes = excessFailures * config.getAccountLockStepMinutes();
      long maxLockMinutes = config.getAccountLockMaxHours() * 60;
      lockMinutes = Math.min(lockMinutes, maxLockMinutes);

      // Set account lock with calculated TTL
      String lockKey = REDIS_LOGIN_LOCKED_PREFIX + username;
      redisTemplate.opsForValue().set(lockKey, failures, lockMinutes, TimeUnit.MINUTES);

      log.warn(
          "Account locked: username={}, failures={}, lockDuration={} minutes",
          username,
          failures,
          lockMinutes);
    }
  }

  /**
   * Handles IP-level failure counting and auto-blocking (non-whitelisted IPs only).
   *
   * <p>Increments the IP failure counter. If the counter exceeds the threshold, the IP is added to
   * the Redis blacklist with the configured block duration.
   *
   * @param clientIp the client IP address
   * @param config the rate limit configuration
   */
  private void handleIpFailure(String clientIp, AppProperties.RateLimit config) {
    String failKey = REDIS_LOGIN_FAIL_IP_PREFIX + clientIp;

    // Increment failure counter
    Long failures = redisTemplate.opsForValue().increment(failKey);
    if (failures == null) {
      return;
    }

    // Set TTL on first failure (sliding window)
    if (failures == 1) {
      redisTemplate.expire(failKey, config.getIpFailWindowMinutes(), TimeUnit.MINUTES);
    }

    log.info("IP login failure recorded: ip={}, failures={}", clientIp, failures);

    // Check if threshold exceeded → auto-block IP
    if (failures >= config.getIpMaxFailures()) {
      String blockKey = REDIS_IP_BLOCKED_PREFIX + clientIp;
      redisTemplate
          .opsForValue()
          .set(blockKey, "auto", config.getIpBlockDurationMinutes(), TimeUnit.MINUTES);

      log.warn(
          "IP auto-blocked: ip={}, failures={}, blockDuration={} minutes",
          clientIp,
          failures,
          config.getIpBlockDurationMinutes());
    }
  }
}
