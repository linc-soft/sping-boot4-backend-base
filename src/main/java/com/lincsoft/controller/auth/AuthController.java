package com.lincsoft.controller.auth;

import com.lincsoft.common.Result;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.controller.auth.vo.ChangePasswordRequest;
import com.lincsoft.controller.auth.vo.ForceChangePasswordRequest;
import com.lincsoft.controller.auth.vo.ForgotPasswordRequest;
import com.lincsoft.controller.auth.vo.LoginRequest;
import com.lincsoft.controller.auth.vo.LoginResponse;
import com.lincsoft.controller.auth.vo.RefreshResponse;
import com.lincsoft.controller.auth.vo.ResetPasswordRequest;
import com.lincsoft.dto.AuthenticatedUserDTO;
import com.lincsoft.i18n.LanguageContext;
import com.lincsoft.services.auth.AuthService;
import com.lincsoft.services.auth.PasswordResetService;
import com.lincsoft.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller.
 *
 * <p>Provides endpoints for user login, logout, and token refresh. Access tokens are returned in
 * the response body, while refresh tokens are managed via HttpOnly cookies.
 *
 * <p>Endpoint security:
 *
 * <ul>
 *   <li>{@code /api/auth/login} — public (no auth, no CSRF)
 *   <li>{@code /api/auth/forgot-password} — public (no auth, no CSRF)
 *   <li>{@code /api/auth/reset-password} — public (no auth, no CSRF)
 *   <li>{@code /api/auth/refresh} — CSRF protected, no auth required
 *   <li>{@code /api/auth/logout} — authenticated + CSRF protected
 *   <li>{@code /api/auth/change-password} — authenticated + CSRF protected
 *   <li>{@code /api/auth/force-change-password} — authenticated + CSRF protected
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  private final PasswordResetService passwordResetService;

  private final AppProperties appProperties;

  /**
   * User login.
   *
   * @param loginRequest the login credentials
   * @param request the HTTP request for extracting client IP (used by login protection)
   * @param response the HTTP response for setting the refresh token cookie
   * @return LoginResponse containing the access token
   */
  @PostMapping("/login")
  public LoginResponse login(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    return authService.login(loginRequest, request, response);
  }

  /**
   * Refresh access token.
   *
   * <p>The refresh token is read from the HttpOnly cookie. A new token pair is issued (Token
   * Rotation) and the old refresh token is revoked.
   *
   * @param request the HTTP request containing the refresh token cookie
   * @param response the HTTP response for setting the new refresh token cookie
   * @return RefreshResponse containing the new access token
   */
  @PostMapping("/refresh")
  public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
    return authService.refresh(request, response);
  }

  /**
   * User logout.
   *
   * <p>Revokes both the access token and refresh token, and clears the refresh token cookie.
   *
   * @param request the HTTP request containing the Authorization header and refresh token cookie
   * @param response the HTTP response for clearing the refresh token cookie
   */
  @PostMapping("/logout")
  public Result<?> logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
    return Result.success();
  }

  /**
   * Forgot password — send a password reset email.
   *
   * <p>For security, this endpoint always returns the same success message regardless of whether
   * the username or email exists in the system (to prevent username enumeration).
   *
   * @param request the forgot password request containing username or email
   * @return success result with generic message
   */
  @PostMapping("/forgot-password")
  public Result<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.sendResetEmail(request.usernameOrEmail(), LanguageContext.getLanguage());
    return Result.successMessage(MessageEnums.SYS_PASSWORD_RESET_EMAIL_SENT);
  }

  /**
   * Reset password using the token from the email link.
   *
   * <p>Validates the token, updates the password, and deletes the token to prevent reuse.
   *
   * @param request the reset password request containing token and new password
   * @return success result
   */
  @PostMapping("/reset-password")
  public Result<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
    return Result.successMessage(MessageEnums.SYS_PASSWORD_RESET_SUCCESS);
  }

  /**
   * Change password for an authenticated user.
   *
   * <p>Requires current password verification before updating.
   *
   * @param request the change password request containing current and new password
   * @return success result
   */
  @PostMapping("/change-password")
  public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    passwordResetService.changePassword(username, request.currentPassword(), request.newPassword());
    return Result.successMessage(MessageEnums.SYS_PASSWORD_CHANGE_SUCCESS);
  }

  /**
   * Force change password for INACTIVE users on first login.
   *
   * <p>INACTIVE users are required to change their password before accessing other resources. After
   * successfully changing the password, the user's status is changed to ENABLED.
   *
   * @param request the force change password request containing the new password
   * @return LoginResponse containing the new access token with ENABLED status
   */
  @PostMapping("/force-change-password")
  public LoginResponse forceChangePassword(@Valid @RequestBody ForceChangePasswordRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    passwordResetService.forceChangePassword(username, request.newPassword());

    // Issue a new access token with ENABLED status so the old INACTIVE-status token is replaced
    String secret = appProperties.getJwt().getSecret();
    AuthenticatedUserDTO userDTO =
        new AuthenticatedUserDTO(username, CommonConstants.USER_STATUS_ACTIVE);
    String accessToken =
        JwtUtil.generateAccessToken(
            username, userDTO, secret, appProperties.getJwt().getExpiration());

    return new LoginResponse(accessToken, false);
  }
}
