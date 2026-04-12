package com.lincsoft.controller.auth;

import com.lincsoft.controller.auth.vo.LoginRequest;
import com.lincsoft.controller.auth.vo.LoginResponse;
import com.lincsoft.controller.auth.vo.RefreshResponse;
import com.lincsoft.services.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 *   <li>{@code /api/auth/refresh} — CSRF protected, no auth required
 *   <li>{@code /api/auth/logout} — authenticated + CSRF protected
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
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    authService.logout(request, response);
  }
}
