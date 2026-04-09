package com.lincsoft.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration properties class.
 *
 * <p>Binds configuration properties from application.yml with the {@code app} prefix. Provides
 * validated access to JWT, CORS, CSRF, and rate limiting settings used throughout the application.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  /**
   * JWT configuration settings.
   *
   * <p>Contains settings used for generating and validating access tokens and refresh tokens,
   * including secret key and expiration times.
   */
  private Jwt jwt = new Jwt();

  /**
   * CORS configuration settings.
   *
   * <p>Defines the allowed origins for cross-origin resource sharing requests.
   */
  private Cors cors = new Cors();

  /**
   * CSRF configuration settings.
   *
   * <p>Controls CSRF cookie security attributes that vary between environments.
   */
  private Csrf csrf = new Csrf();

  /**
   * Rate limiting configuration settings.
   *
   * <p>Defines rate limit parameters for API endpoint protection.
   */
  private RateLimit rateLimit = new RateLimit();

  /**
   * Cache configuration settings.
   *
   * <p>Defines TTL values for various cache entries used throughout the application.
   */
  private Cache cache = new Cache();

  /**
   * Security configuration settings.
   *
   * <p>Defines endpoint whitelist patterns for authentication and CSRF protection.
   */
  private Security security = new Security();

  /**
   * Validates JWT secret key length on application startup.
   *
   * <p>The HS256 algorithm requires a minimum key length of 256 bits (32 bytes). This method
   * ensures the secret key meets this requirement and throws an {@link IllegalStateException} if
   * validation fails, preventing the application from starting with an insecure configuration.
   *
   * @throws IllegalStateException if the secret key is null or less than 32 characters
   */
  @PostConstruct
  void validate() {
    // Validate JWT secret key length
    if (jwt.secret == null || jwt.secret.length() < 32) {
      throw new IllegalStateException(
          "app.jwt.secret must be at least 32 characters long. Current length: "
              + (jwt.secret == null ? 0 : jwt.secret.length()));
    }
    // Validate CORS allowed origins
    validateCorsOrigins();
  }

  /**
   * Validates CORS allowed origins configuration.
   *
   * <p>Ensures that each origin value is a valid URL without wildcards. Wildcard origins ({@code
   * *}) are not allowed when {@code AllowCredentials=true}, and pattern-based origins (e.g., {@code
   * *.example.com}) must use {@code allowedOriginPatterns} instead of {@code allowedOrigins}.
   *
   * @throws IllegalStateException if any origin is blank, contains wildcards, or is not a valid URL
   */
  private void validateCorsOrigins() {
    List<String> origins = cors.getAllowedOrigins();
    if (origins == null || origins.isEmpty()) {
      throw new IllegalStateException("app.cors.allowed-origins must not be empty.");
    }
    for (String origin : origins) {
      String trimmed = getTrimmedOrigin(origin);
      try {
        URI uri = new URI(trimmed);
        if (uri.getScheme() == null || uri.getHost() == null) {
          throw new IllegalStateException(
              "app.cors.allowed-origins must include scheme and host. Invalid: '" + trimmed + "'");
        }
      } catch (URISyntaxException e) {
        throw new IllegalStateException(
            "app.cors.allowed-origins contains an invalid URL: '" + trimmed + "'", e);
      }
    }
  }

  /**
   * Trims and validates a CORS origin entry.
   *
   * @param origin the raw origin string from configuration
   * @return the trimmed origin string
   * @throws IllegalStateException if the origin is blank or contains wildcards
   */
  private static @NonNull String getTrimmedOrigin(String origin) {
    String trimmedOrigin = origin.trim();
    if (trimmedOrigin.isBlank()) {
      throw new IllegalStateException("app.cors.allowed-origins must not contain blank entries.");
    }
    // Reject wildcard origins as they are incompatible with AllowCredentials=true
    if (trimmedOrigin.contains("*")) {
      throw new IllegalStateException(
          "app.cors.allowed-origins must not contain wildcards ('*'). "
              + "Wildcard origins are incompatible with AllowCredentials=true. "
              + "Found: '"
              + trimmedOrigin
              + "'");
    }
    return trimmedOrigin;
  }

  /**
   * JWT configuration inner class.
   *
   * <p>Binds properties under the {@code app.jwt} prefix from configuration files. Contains
   * settings for token generation and validation.
   */
  @Data
  public static class Jwt {
    /**
     * Secret key for HMAC-SHA256 signing.
     *
     * <p>In production environments, this should be overridden via environment variables or a
     * secret management tool. Must be at least 32 characters long.
     */
    private String secret = null;

    /**
     * Access token expiration time in milliseconds.
     *
     * <p>Default: 10 minutes (600,000 ms)
     */
    private long expiration = 600000L;

    /**
     * Refresh token expiration time in milliseconds.
     *
     * <p>Default: 1 day (86,400,000 ms)
     */
    private long refreshExpiration = 86400000L;
  }

  /**
   * CORS configuration inner class.
   *
   * <p>Binds properties under the {@code app.cors} prefix from configuration files. Defines
   * cross-origin resource sharing settings for the application.
   */
  @Data
  public static class Cors {
    /**
     * Allowed CORS origins (list of origin URLs).
     *
     * <p>Example: {@code ["http://localhost:5173", "https://www.example.com"]} These origins will
     * be permitted to make cross-origin requests to the API.
     */
    private List<String> allowedOrigins = List.of("http://localhost:5173");
  }

  /**
   * CSRF configuration inner class.
   *
   * <p>Binds properties under the {@code app.csrf} prefix from configuration files. Controls
   * security attributes of the CSRF cookie that differ between development and production.
   */
  @Data
  public static class Csrf {
    /**
     * Whether the CSRF cookie should have the Secure flag.
     *
     * <p>When {@code true}, the cookie is only sent over HTTPS connections. Should be {@code false}
     * in development (HTTP localhost) and {@code true} in production (HTTPS).
     *
     * <p>Default: false (development-safe)
     */
    private boolean secure = false;
  }

  /**
   * Cache configuration inner class.
   *
   * <p>Binds properties under the {@code app.cache} prefix from configuration files. Defines TTL
   * values for different cache entries, allowing environment-specific tuning.
   */
  @Data
  public static class Cache {
    /**
     * Default cache entry TTL in hours.
     *
     * <p>Applied to all caches that do not have a specific TTL override. Default: 1 hour.
     */
    private long defaultTtlHours = 1;

    /**
     * UserDetails cache entry TTL in minutes.
     *
     * <p>Controls how long authenticated user details are cached in Redis. A shorter TTL ensures
     * user state changes (role updates, deactivation) are reflected promptly. Default: 2 minutes.
     */
    private long userDetailsTtlMinutes = 2;
  }

  /**
   * Rate limiting configuration inner class.
   *
   * <p>Binds properties under the {@code app.rate-limit} prefix from configuration files. Controls
   * the token-bucket rate limiting parameters for API endpoints.
   */
  @Data
  public static class RateLimit {
    /**
     * Whether rate limiting is enabled.
     *
     * <p>Default: true
     */
    private boolean enabled = true;

    /**
     * Maximum number of requests allowed in the time window (bucket capacity).
     *
     * <p>Default: 50 requests
     */
    private int capacity = 50;

    /**
     * Number of tokens refilled per refill period.
     *
     * <p>Default: 50 tokens
     */
    private int refillTokens = 50;

    /**
     * Refill period in seconds.
     *
     * <p>Default: 60 seconds (1 minute)
     */
    private long refillPeriodSeconds = 60;
  }

  /**
   * Security configuration inner class.
   *
   * <p>Binds properties under the {@code app.security} prefix. Defines endpoint whitelist patterns
   * that control which paths bypass authentication or CSRF protection.
   */
  @Data
  public static class Security {
    /**
     * Public endpoints that bypass both authentication and CSRF protection.
     *
     * <p>These endpoints can be accessed without authentication and do not require CSRF tokens.
     *
     * <p>Default: login endpoint and API documentation
     */
    private String[] publicEndpoints = {"/api/auth/login"};

    /**
     * Endpoints that require CSRF protection but bypass authentication.
     *
     * <p>These endpoints are accessible without authentication but still require valid CSRF tokens
     * for state-changing operations.
     *
     * <p>Default: token refresh endpoint
     */
    private String[] authOnlyWhitelist = {"/api/auth/refresh"};

    /**
     * Whether to include the HSTS preload directive.
     *
     * <p>When {@code true}, the Strict-Transport-Security header includes the {@code preload}
     * directive, allowing the domain to be submitted to browser HSTS preload lists. This eliminates
     * the vulnerability window on first visit but requires the domain to always use HTTPS.
     *
     * <p>Should be {@code false} in development and {@code true} in production only after
     * confirming 100% HTTPS coverage.
     *
     * <p>Default: false (development-safe)
     */
    private boolean hstsPreload = false;
  }
}
