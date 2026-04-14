package com.lincsoft.services.auth;

import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.controller.auth.vo.LoginRequest;
import com.lincsoft.controller.auth.vo.LoginResponse;
import com.lincsoft.controller.auth.vo.RefreshResponse;
import com.lincsoft.dto.AuthenticatedUserDTO;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.filter.PreAuthenticationChecks;
import com.lincsoft.services.master.UserService;
import com.lincsoft.services.system.TokenBlacklistService;
import com.lincsoft.util.JwtUtil;
import com.lincsoft.util.LogUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Authentication Service.
 *
 * <p>Handles login, logout, and token refresh business logic. Uses Spring Security's {@link
 * AuthenticationManager} for credential validation. Access tokens are returned in the response
 * body, while refresh tokens are delivered via HttpOnly cookies for security.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final UserService userService;
  private final AppProperties appProperties;
  private final TokenBlacklistService tokenBlacklistService;
  private final LoginProtectionService loginProtectionService;

  /**
   * Authenticate user via {@link AuthenticationManager} and generate access + refresh tokens.
   *
   * <p>Delegates credential validation to Spring Security's authentication chain
   * (DaoAuthenticationProvider → UserDetailsService → PasswordEncoder). On success, generates a
   * token pair with username as the JWT subject.
   *
   * <p>Includes brute-force protection:
   *
   * <ul>
   *   <li>Lock check: {@link PreAuthenticationChecks} performs a real-time account lock check from
   *       Redis before password validation. If the account is locked, {@link LockedException} is
   *       thrown by the pre-authentication checker.
   *   <li>On failure: Records the failure for both account and IP dimensions via {@link
   *       LoginProtectionService}.
   *   <li>On success: Clears all failure counters for the account and IP.
   * </ul>
   *
   * <p><b>Security note — username enumeration prevention:</b> Both {@link LockedException} and
   * {@link BadCredentialsException} are caught together and mapped to the same {@code
   * INVALID_CREDENTIALS} error code. This ensures that "account locked", "wrong password", and
   * "user not found" are indistinguishable to the caller, preventing username enumeration via
   * distinct error codes. Additionally, because the lock check happens inside {@code
   * DaoAuthenticationProvider} (via {@code PreAuthenticationChecks}, after {@code
   * loadUserByUsername}), Spring Security's built-in timing-attack mitigation (dummy password hash
   * on user-not-found) remains active for all paths.
   *
   * @param request the login request containing username and password
   * @param httpRequest the HTTP servlet request for extracting client IP
   * @param response the HTTP response for setting the refresh token cookie
   * @return LoginResponse containing the access token
   */
  public LoginResponse login(
      LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
    String username = request.username();
    String clientIp = LogUtil.getClientIp(httpRequest);

    try {
      // Delegate authentication to Spring Security's AuthenticationManager.
      // DaoAuthenticationProvider calls loadUserByUsername(), then PreAuthenticationChecks
      // verifies lock status from Redis. If locked, LockedException is thrown;
      // if credentials wrong, BadCredentialsException.
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, request.password()));

      // Extract authenticated user details
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      String authenticatedUsername = userDetails.getUsername();

      // Generate tokens with username as subject
      String secret = appProperties.getJwt().getSecret();
      AuthenticatedUserDTO userDTO =
          new AuthenticatedUserDTO(authenticatedUsername, CommonConstants.USER_STATUS_ACTIVE);

      String accessToken =
          JwtUtil.generateAccessToken(
              authenticatedUsername, userDTO, secret, appProperties.getJwt().getExpiration());
      String refreshToken =
          JwtUtil.generateRefreshToken(
              authenticatedUsername, secret, appProperties.getJwt().getRefreshExpiration());

      // Register active session (kicks off previous session if exists)
      Claims refreshClaims = JwtUtil.parseToken(refreshToken, secret);
      tokenBlacklistService.registerSession(
          authenticatedUsername,
          refreshClaims.getId(),
          appProperties.getJwt().getRefreshExpiration());

      // Set refresh token as HttpOnly cookie
      setRefreshTokenCookie(response, refreshToken, appProperties.getJwt().getRefreshExpiration());

      // Clear failure counters on successful login
      loginProtectionService.recordSuccess(authenticatedUsername, clientIp);

      log.info("User logged in successfully: username={}", authenticatedUsername);
      return new LoginResponse(accessToken);
    } catch (LockedException | BadCredentialsException e) {
      // Record login failure for brute-force protection.
      // Both LockedException (account locked) and BadCredentialsException (wrong password /
      // user not found) are mapped to the same INVALID_CREDENTIALS error to prevent username
      // enumeration — callers cannot distinguish between these two cases.
      loginProtectionService.recordFailure(username, clientIp);
      throw new BusinessException(MessageEnums.INVALID_CREDENTIALS);
    }
  }

  /**
   * Refresh the access token using the refresh token from the cookie.
   *
   * <p>Implements Token Rotation: the old refresh token is revoked and a new one is issued. Reloads
   * user details via {@link UserDetailsService} to verify the user is still active.
   *
   * @param request the HTTP request containing the refresh token cookie
   * @param response the HTTP response for setting the new refresh token cookie
   * @return RefreshResponse containing the new access token
   * @throws BusinessException if the refresh token is missing, invalid, or revoked
   */
  public RefreshResponse refresh(HttpServletRequest request, HttpServletResponse response) {
    // Extract refresh token from cookie
    String refreshToken = extractRefreshTokenFromCookie(request);
    if (refreshToken == null) {
      throw new BusinessException(MessageEnums.INVALID_REFRESH_TOKEN);
    }

    Claims claims;
    try {
      claims = JwtUtil.parseToken(refreshToken, appProperties.getJwt().getSecret());
    } catch (JwtException e) {
      log.warn("Refresh token parsing failed: {}", e.getMessage());
      throw new BusinessException(MessageEnums.INVALID_REFRESH_TOKEN);
    }

    // Verify token type is refresh
    String tokenType = claims.get(CommonConstants.JWT_CLAIM_TOKEN_TYPE_KEY, String.class);
    if (!CommonConstants.TOKEN_TYPE_REFRESH.equals(tokenType)) {
      throw new BusinessException(MessageEnums.INVALID_REFRESH_TOKEN);
    }

    // Check if the refresh token has been revoked
    String jti = claims.getId();
    if (jti != null && tokenBlacklistService.isTokenRevoked(jti)) {
      log.warn("Refresh token has been revoked: jti={}", jti);
      throw new BusinessException(MessageEnums.INVALID_REFRESH_TOKEN);
    }

    // Token Rotation: revoke the old refresh token
    if (jti != null) {
      long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
      if (remainingMillis > 0) {
        tokenBlacklistService.revokeToken(jti, remainingMillis);
      }
    }

    // Reload user details to verify user is still active
    // This call goes through UserDetailsService (with @Cacheable) and will throw
    // BusinessException if user is not found or inactive
    String username = claims.getSubject();
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // Generate new token pair
    String secret = appProperties.getJwt().getSecret();
    AuthenticatedUserDTO userDTO =
        new AuthenticatedUserDTO(userDetails.getUsername(), CommonConstants.USER_STATUS_ACTIVE);

    String newAccessToken =
        JwtUtil.generateAccessToken(
            username, userDTO, secret, appProperties.getJwt().getExpiration());
    String newRefreshToken =
        JwtUtil.generateRefreshToken(
            username, secret, appProperties.getJwt().getRefreshExpiration());

    // Update active session with new refresh token JTI
    Claims newRefreshClaims = JwtUtil.parseToken(newRefreshToken, secret);
    tokenBlacklistService.registerSession(
        username, newRefreshClaims.getId(), appProperties.getJwt().getRefreshExpiration());

    // Set new refresh token cookie
    setRefreshTokenCookie(response, newRefreshToken, appProperties.getJwt().getRefreshExpiration());

    log.info("Token refreshed successfully: username={}", username);
    return new RefreshResponse(newAccessToken);
  }

  /**
   * Logout the user by revoking both access and refresh tokens.
   *
   * @param request the HTTP request containing the Authorization header and refresh token cookie
   * @param response the HTTP response for clearing the refresh token cookie
   */
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    String secret = appProperties.getJwt().getSecret();
    String username = null;

    // Revoke access token from Authorization header
    String authHeader = request.getHeader(CommonConstants.AUTHORIZATION_HEADER);
    if (authHeader != null && authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
      String accessToken = authHeader.substring(CommonConstants.BEARER_PREFIX.length());
      try {
        Claims claims = JwtUtil.parseToken(accessToken, secret);
        username = claims.getSubject();
        String jti = claims.getId();
        if (jti != null) {
          long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
          if (remainingMillis > 0) {
            tokenBlacklistService.revokeToken(jti, remainingMillis);
          }
        }
      } catch (JwtException e) {
        log.warn("Failed to revoke access token during logout: {}", e.getMessage());
      }
    }

    // Revoke refresh token from cookie
    String refreshToken = extractRefreshTokenFromCookie(request);
    if (refreshToken != null) {
      revokeTokenSafely(refreshToken, secret, "refresh");
    }

    // Clear refresh token cookie
    clearRefreshTokenCookie(response);

    // Remove active session record
    if (username != null) {
      tokenBlacklistService.removeSession(username);
      // Evict cached UserDetails so next login loads fresh data from DB
      userService.evictUserDetailsCache(username);
    }

    log.info("User logged out successfully");
  }

  /**
   * Safely revoke a token by adding its JTI to the blacklist. Exceptions during revocation are
   * logged but do not interrupt the logout flow.
   */
  private void revokeTokenSafely(String token, String secret, String tokenType) {
    try {
      Claims claims = JwtUtil.parseToken(token, secret);
      String jti = claims.getId();
      if (jti != null) {
        long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingMillis > 0) {
          tokenBlacklistService.revokeToken(jti, remainingMillis);
        }
      }
    } catch (JwtException e) {
      log.warn("Failed to revoke {} token during logout: {}", tokenType, e.getMessage());
    }
  }

  /** Extract the refresh token value from the request cookies. */
  private String extractRefreshTokenFromCookie(HttpServletRequest request) {
    if (request.getCookies() == null) {
      return null;
    }
    return Arrays.stream(request.getCookies())
        .filter(c -> CommonConstants.REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
        .map(Cookie::getValue)
        .findFirst()
        .orElse(null);
  }

  /** Set the refresh token as an HttpOnly, Secure, SameSite=Strict cookie. */
  private void setRefreshTokenCookie(
      HttpServletResponse response, String refreshToken, long maxAgeMillis) {
    ResponseCookie cookie =
        ResponseCookie.from(CommonConstants.REFRESH_TOKEN_COOKIE_NAME, refreshToken)
            .httpOnly(true)
            .secure(appProperties.getCsrf().isSecure())
            .sameSite("Strict")
            .path(CommonConstants.REFRESH_TOKEN_COOKIE_PATH)
            .maxAge(maxAgeMillis / 1000)
            .build();
    response.addHeader("Set-Cookie", cookie.toString());
  }

  /** Clear the refresh token cookie by setting Max-Age=0. */
  private void clearRefreshTokenCookie(HttpServletResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(CommonConstants.REFRESH_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(appProperties.getCsrf().isSecure())
            .sameSite("Strict")
            .path(CommonConstants.REFRESH_TOKEN_COOKIE_PATH)
            .maxAge(0)
            .build();
    response.addHeader("Set-Cookie", cookie.toString());
  }
}
