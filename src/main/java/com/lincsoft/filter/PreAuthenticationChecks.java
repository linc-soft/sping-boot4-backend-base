package com.lincsoft.filter;

import com.lincsoft.services.auth.AuthService;
import com.lincsoft.services.auth.LoginProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

/**
 * Custom Pre-Authentication Checks for account lock status.
 *
 * <p>This checker is invoked by {@link DaoAuthenticationProvider} <b>before</b> password
 * validation. It performs real-time account lock checks from Redis, ensuring that lock/unlock
 * status changes take effect immediately without being affected by UserDetails caching.
 *
 * <p>Key design decisions:
 *
 * <ul>
 *   <li>Lock status is checked on every authentication attempt (not cached)
 *   <li>Works alongside Spring Cache for UserDetails (caches static data, checks dynamic state)
 *   <li>Throws {@link LockedException} which is caught in {@link AuthService} and mapped to {@code
 *       INVALID_CREDENTIALS} to prevent username enumeration
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreAuthenticationChecks implements UserDetailsChecker {
  private final LoginProtectionService loginProtectionService;

  /**
   * Checks if the user account is locked before authentication proceeds.
   *
   * <p>This method is called by DaoAuthenticationProvider after loading UserDetails but before
   * password validation. If the account is locked, a {@link LockedException} is thrown, which
   * Spring Security propagates up the authentication chain.
   *
   * @param user the UserDetails object loaded from UserDetailsService
   * @throws LockedException if the account is currently locked in Redis
   */
  @Override
  public void check(UserDetails user) {
    if (loginProtectionService.isAccountLocked(user.getUsername())) {
      log.warn("Pre-authentication lock check failed: username={}", user.getUsername());
      throw new LockedException("Account is locked");
    }
  }
}
