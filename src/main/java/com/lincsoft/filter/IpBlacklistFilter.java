package com.lincsoft.filter;

import com.lincsoft.common.Result;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.util.IpChecker;
import com.lincsoft.util.LogUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * IP Blacklist Enforcement Filter.
 *
 * <p>Checks whether the client IP is currently blacklisted in Redis (auto-blocked by {@link
 * com.lincsoft.services.auth.LoginProtectionService} after exceeding the login failure threshold).
 * Blacklisted IPs are rejected with 403 Forbidden regardless of any other configuration.
 *
 * <p>This filter is intentionally decoupled from {@link RateLimitFilter} so that the blacklist
 * remains active even when rate limiting is disabled (e.g., in development environments).
 *
 * <p>Processing order:
 *
 * <ol>
 *   <li>Whitelisted IPs bypass the blacklist check entirely.
 *   <li>Non-whitelisted IPs are checked against the Redis blacklist via {@link IpChecker}.
 *   <li>Blacklisted IPs receive a 403 Forbidden response immediately.
 *   <li>All other IPs proceed to the next filter.
 * </ol>
 *
 * @author 林创科技
 * @since 2026-04-11
 */
@Slf4j
@RequiredArgsConstructor
public class IpBlacklistFilter extends OncePerRequestFilter {

  /** Object mapper for JSON response serialization. */
  private final ObjectMapper objectMapper;

  /**
   * Checks the client IP against the whitelist and Redis blacklist, rejecting blocked IPs.
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

    // Resolve client IP address (proxy-aware)
    String clientIp = LogUtil.getClientIp(request);

    // Whitelisted IPs bypass the blacklist check
    if (IpChecker.isWhitelisted(clientIp)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Reject blacklisted IPs with 403 Forbidden
    if (IpChecker.isBlocked(clientIp)) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType("application/json;charset=UTF-8");
      response
          .getWriter()
          .write(objectMapper.writeValueAsString(Result.error(MessageEnums.IP_BLOCKED)));
      log.warn("Request blocked from blacklisted IP: {}", clientIp);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
