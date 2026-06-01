package com.lincsoft.services.auth;

import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstUserMapper;
import com.lincsoft.services.master.UserService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Password reset service.
 *
 * <p>Handles forgot password, reset password, and change password flows. Uses Redis for token
 * storage with configurable TTL.
 *
 * @author 林创科技
 * @since 2026-05-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final MstUserMapper userMapper;
  private final UserService userService;
  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate stringRedisTemplate;
  private final EmailService emailService;
  private final AppProperties appProperties;

  /**
   * Send a password reset email if the user exists and has an email address.
   *
   * <p>For security, this method never reveals whether a user exists. The response is always the
   * same success message regardless of whether the user was found.
   *
   * @param usernameOrEmail username or email address entered by the user
   * @param locale locale string for email template i18n
   */
  public void sendResetEmail(String usernameOrEmail, String locale) {
    // Rate limiting: max 3 requests per 5 minutes per identifier
    String rateKey =
        CommonConstants.REDIS_PASSWORD_RESET_RATE_PREFIX + usernameOrEmail.toLowerCase();
    String rateValue = stringRedisTemplate.opsForValue().get(rateKey);
    int count = rateValue != null ? Integer.parseInt(rateValue) : 0;
    if (count >= 3) {
      log.debug("Password reset rate limit exceeded for identifier: {}", usernameOrEmail);
      return;
    }
    stringRedisTemplate
        .opsForValue()
        .set(rateKey, String.valueOf(count + 1), Duration.ofMinutes(5));

    // Find user by username first, then by email
    MstUser user = userService.findByUsernameOrEmail(usernameOrEmail);

    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      log.debug("No user with email found for reset request");
      return;
    }

    // Invalidate any existing token for this user (only latest token is valid)
    String userTokenKey =
        CommonConstants.REDIS_PASSWORD_RESET_USER_PREFIX + user.getUsername().toLowerCase();
    String existingToken = stringRedisTemplate.opsForValue().get(userTokenKey);
    if (existingToken != null) {
      stringRedisTemplate.delete(CommonConstants.REDIS_PASSWORD_RESET_PREFIX + existingToken);
    }

    // Generate secure random token
    String token = UUID.randomUUID().toString().replace("-", "");
    long ttlMinutes = appProperties.getPasswordReset().getTokenTtlMinutes();

    // Store token in Redis: key = password:reset:{token}, value = username
    String tokenKey = CommonConstants.REDIS_PASSWORD_RESET_PREFIX + token;
    stringRedisTemplate
        .opsForValue()
        .set(tokenKey, user.getUsername(), Duration.ofMinutes(ttlMinutes));

    // Store user→current token mapping
    stringRedisTemplate.opsForValue().set(userTokenKey, token, Duration.ofMinutes(ttlMinutes));

    // Build reset link
    String baseUrl = appProperties.getPasswordReset().getBaseUrl();
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    String resetLink = baseUrl + "/reset-password?token=" + token;

    // Send email asynchronously
    emailService.sendPasswordResetEmail(user.getEmail(), resetLink, ttlMinutes, locale);

    log.info("Password reset email sent for user: {}", user.getUsername());
  }

  /**
   * Reset password using the token from the email link.
   *
   * <p>Validates the token, retrieves the associated username, updates the password, and deletes
   * the token to prevent reuse.
   *
   * @param token reset token from the email link
   * @param newPassword new password to set
   * @throws BusinessException if the token is invalid or expired
   */
  public void resetPassword(String token, String newPassword) {
    String tokenKey = CommonConstants.REDIS_PASSWORD_RESET_PREFIX + token;
    String username = stringRedisTemplate.opsForValue().get(tokenKey);

    if (username == null) {
      throw new BusinessException(MessageEnums.SYS_PASSWORD_RESET_TOKEN_INVALID);
    }

    // Delete token immediately to prevent reuse
    stringRedisTemplate.delete(tokenKey);

    // Clean up user→token mapping
    String userTokenKey = CommonConstants.REDIS_PASSWORD_RESET_USER_PREFIX + username.toLowerCase();
    stringRedisTemplate.delete(userTokenKey);

    // Update password in database
    String encodedPassword = passwordEncoder.encode(newPassword);
    MstUser user = userService.findByUsername(username);
    if (user == null) {
      throw new BusinessException(MessageEnums.SYS_PASSWORD_RESET_TOKEN_INVALID);
    }

    user.setPassword(encodedPassword);
    userMapper.updateById(user);

    // Evict cached UserDetails so next login loads the new password
    userService.evictUserDetailsCache(username);

    log.info("Password reset successful for user: {}", username);
  }

  /**
   * Force change password for INACTIVE users on first login.
   *
   * <p>Sets the new password and changes the user's status from INACTIVE to ENABLED. Also evicts
   * the UserDetails cache so the status change takes effect immediately.
   *
   * @param username the authenticated username
   * @param newPassword the new password to set
   * @throws BusinessException if the user is not found or not in INACTIVE status
   */
  public void forceChangePassword(String username, String newPassword) {
    MstUser user = userService.findByUsername(username);
    if (user == null) {
      throw new BusinessException(MessageEnums.USER_NOT_FOUND);
    }

    // Only INACTIVE users can use force-change-password
    if (!CommonConstants.USER_STATUS_INACTIVE.equals(user.getStatus())) {
      throw new BusinessException(MessageEnums.FORBIDDEN);
    }

    // Encode and set new password
    String encodedPassword = passwordEncoder.encode(newPassword);
    user.setPassword(encodedPassword);

    // Change status from INACTIVE to ENABLED
    user.setStatus(CommonConstants.USER_STATUS_ACTIVE);

    userMapper.updateById(user);

    // Evict cached UserDetails so next authentication loads fresh data with ENABLED status
    userService.evictUserDetailsCache(username);

    log.info("Force password change successful for user: {}", username);
  }

  /**
   * Change password for an authenticated user.
   *
   * <p>Verifies the current password before updating.
   *
   * @param username authenticated username
   * @param currentPassword current password for verification
   * @param newPassword new password to set
   * @throws BusinessException if the current password is incorrect
   */
  public void changePassword(String username, String currentPassword, String newPassword) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    if (!passwordEncoder.matches(currentPassword, userDetails.getPassword())) {
      throw new BusinessException(MessageEnums.SYS_CURRENT_PASSWORD_MISMATCH);
    }

    String encodedPassword = passwordEncoder.encode(newPassword);
    MstUser user = userService.findByUsername(username);
    if (user == null) {
      throw new BusinessException(MessageEnums.USER_NOT_FOUND);
    }

    user.setPassword(encodedPassword);
    userMapper.updateById(user);

    // Evict cached UserDetails so next authentication uses the new password
    userService.evictUserDetailsCache(username);

    log.info("Password changed successfully for user: {}", username);
  }
}
