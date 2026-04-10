package com.lincsoft.filter;

import com.lincsoft.common.Result;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.services.system.TokenBlacklistService;
import com.lincsoft.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * JWT Authorization Filter.
 *
 * <p>This filter intercepts incoming HTTP requests to validate JWT tokens from the Authorization
 * header. If a valid token is present, it extracts the user identity, loads user details, and sets
 * the authentication object in the Spring Security context. Invalid or missing tokens result in a
 * 401 Unauthorized response.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Extract and validate JWT tokens from Authorization header
 *   <li>Check token revocation status via Redis blacklist
 *   <li>Set authentication context for authorized users
 *   <li>Set current user in MDC for logging and audit trails
 *   <li>Handle JWT exceptions with appropriate error responses
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-09
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

  /**
   * User details service for loading user information.
   *
   * <p>Used to retrieve user details from the database based on the username extracted from the JWT
   * token.
   */
  private final UserDetailsService userDetailsService;

  /**
   * Application configuration properties.
   *
   * <p>Provides access to JWT settings such as the secret key and token expiration times.
   */
  private final AppProperties appProperties;

  /** Object Mapper for JSON processing. */
  private final ObjectMapper objectMapper;

  /**
   * Token blacklist service for checking JWT revocation status.
   *
   * <p>Verifies whether a token's JTI has been blacklisted (e.g., due to user logout or account
   * deactivation).
   */
  private final TokenBlacklistService tokenBlacklistService;

  /**
   * Processes the incoming HTTP request to validate JWT authentication.
   *
   * <p>Execution flow:
   *
   * <ol>
   *   <li>Extract the Authorization header from the request
   *   <li>If no Bearer token is present, skip to the next filter
   *   <li>Parse and validate the JWT token
   *   <li>Check if the token has been revoked (blacklisted)
   *   <li>Load user details and set authentication in SecurityContext
   *   <li>Set username in MDC for logging purposes
   *   <li>Continue the filter chain
   *   <li>Clean up MDC in finally block to prevent thread pool contamination
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
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Extract the Bearer token from the Authorization header
    String authHeader = request.getHeader(CommonConstants.AUTHORIZATION_HEADER);

    // If Bearer token is not present, skip to the next filter
    if (authHeader == null || !authHeader.startsWith(CommonConstants.BEARER_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Remove the "Bearer " prefix to extract the raw token
    String token = authHeader.substring(CommonConstants.BEARER_PREFIX.length());

    try {
      // Parse the JWT token and retrieve the claims
      Claims claims = JwtUtil.parseToken(token, appProperties.getJwt().getSecret());

      // Reject non-access tokens (e.g., refresh tokens must not be used as access tokens)
      String tokenType = claims.get(CommonConstants.JWT_CLAIM_TOKEN_TYPE_KEY, String.class);
      if (!CommonConstants.TOKEN_TYPE_ACCESS.equals(tokenType)) {
        log.warn("Non-access token used in Authorization header: type={}", tokenType);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response
            .getWriter()
            .write(objectMapper.writeValueAsString(Result.error(MessageEnums.UNAUTHORIZED)));
        return;
      }

      // Get the username from the token subject
      String subject = claims.getSubject();

      // Check if the token has been revoked (blacklisted)
      String jti = claims.getId();
      if (jti != null && tokenBlacklistService.isTokenRevoked(jti)) {
        log.warn("JWT token has been revoked: jti={}", jti);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response
            .getWriter()
            .write(objectMapper.writeValueAsString(Result.error(MessageEnums.TOKEN_REVOKED)));
        return;
      }

      // If subject is valid and SecurityContext is not yet authenticated, proceed with
      // authentication
      if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {

        // Load user details from the UserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername(subject);

        // Create authentication token and set it in the SecurityContext
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Set username in MDC for logging and audit purposes (used in MetaObjectHandler, etc.)
        MDC.put(CommonConstants.MDC_CURRENT_USER_KEY, userDetails.getUsername());
      }
    } catch (JwtException e) {
      log.warn("JWT authentication failed: {}", e.getMessage());
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json;charset=UTF-8");
      response
          .getWriter()
          .write(objectMapper.writeValueAsString(Result.error(MessageEnums.UNAUTHORIZED)));
      return;
    }

    // Continue with the filter chain
    try {
      filterChain.doFilter(request, response);
    } finally {
      // Clean up MDC to prevent cross-request contamination in thread pools
      MDC.remove(CommonConstants.MDC_CURRENT_USER_KEY);
    }
  }
}
