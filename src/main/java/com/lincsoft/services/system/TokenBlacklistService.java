package com.lincsoft.services.system;

import com.lincsoft.constant.CommonConstants;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * JWT Token Blacklist Service.
 *
 * <p>Manages a Redis-based token blacklist to support JWT token revocation. When a token needs to
 * be invalidated (e.g., user logout, password change, account deactivation), its JTI (JWT ID) is
 * added to the blacklist with a TTL equal to the token's remaining validity period.
 *
 * <p>Key operations:
 *
 * <ul>
 *   <li>Revoke a token by adding its JTI to the Redis blacklist
 *   <li>Check if a token has been revoked by looking up its JTI
 *   <li>Automatic cleanup via Redis TTL (expired entries are removed automatically)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

  /** Redis template for string key-value operations. */
  private final StringRedisTemplate stringRedisTemplate;

  /**
   * Adds a token's JTI to the blacklist with a TTL matching its remaining validity.
   *
   * <p>The TTL ensures the blacklist entry is automatically removed after the token would have
   * expired naturally, preventing unbounded growth of the blacklist.
   *
   * @param jti the JWT ID to blacklist
   * @param remainingMillis the remaining validity time of the token in milliseconds
   */
  public void revokeToken(String jti, long remainingMillis) {
    if (jti == null || jti.isBlank()) {
      return;
    }
    String key = CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + jti;
    // Store "revoked" as value with TTL = remaining token validity
    stringRedisTemplate.opsForValue().set(key, "revoked", remainingMillis, TimeUnit.MILLISECONDS);
    log.debug("Token revoked: jti={}, ttl={}ms", jti, remainingMillis);
  }

  /**
   * Checks whether a token has been revoked.
   *
   * @param jti the JWT ID to check
   * @return {@code true} if the token is blacklisted (revoked), {@code false} otherwise
   */
  public boolean isTokenRevoked(String jti) {
    if (jti == null || jti.isBlank()) {
      return false;
    }
    String key = CommonConstants.REDIS_TOKEN_BLACKLIST_PREFIX + jti;
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
  }

  /**
   * Registers an active session for the given user by storing the refresh token's JTI. If the user
   * already has an active session, the previous refresh token is revoked (kicked off).
   *
   * @param username the username of the authenticated user
   * @param newJti the JTI of the new refresh token
   * @param refreshExpirationMillis the TTL for the session entry (matches refresh token lifetime)
   */
  public void registerSession(String username, String newJti, long refreshExpirationMillis) {
    String sessionKey = CommonConstants.REDIS_ACTIVE_SESSION_PREFIX + username;

    // Check for existing active session and revoke it
    String oldJti = stringRedisTemplate.opsForValue().get(sessionKey);
    if (oldJti != null && !oldJti.isBlank()) {
      // Revoke the old refresh token with the remaining TTL of the session key
      Long remainingMillis = stringRedisTemplate.getExpire(sessionKey, TimeUnit.MILLISECONDS);
      if (remainingMillis != null && remainingMillis > 0) {
        revokeToken(oldJti, remainingMillis);
        log.info("Kicked off previous session: username={}, oldJti={}", username, oldJti);
      }
    }

    // Store the new session
    stringRedisTemplate
        .opsForValue()
        .set(sessionKey, newJti, refreshExpirationMillis, TimeUnit.MILLISECONDS);
    log.debug("Session registered: username={}, jti={}", username, newJti);
  }

  /**
   * Removes the active session record for the given user.
   *
   * @param username the username whose session should be cleared
   */
  public void removeSession(String username) {
    String sessionKey = CommonConstants.REDIS_ACTIVE_SESSION_PREFIX + username;
    stringRedisTemplate.delete(sessionKey);
    log.debug("Session removed: username={}", username);
  }
}
