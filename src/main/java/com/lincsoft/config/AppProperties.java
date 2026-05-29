package com.lincsoft.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
   * Access log configuration settings.
   *
   * <p>Defines path exclusion patterns for the access log interceptor and batch write parameters.
   */
  private AccessLog accessLog = new AccessLog();

  /**
   * Async thread pool configuration settings.
   *
   * <p>Defines thread pool parameters for asynchronous log saving operations.
   */
  private Async async = new Async();

  /**
   * I18n internationalization configuration settings.
   *
   * <p>Defines default locale and supported locales for internationalization.
   */
  private I18n i18n = new I18n();

  /**
   * Report configuration settings.
   *
   * <p>Defines PDF report generation parameters including font paths and export limits.
   */
  private Report report = new Report();

  /**
   * Password reset configuration settings.
   *
   * <p>Defines password reset token TTL and frontend base URL for generating reset links.
   */
  private PasswordReset passwordReset = new PasswordReset();

  /**
   * Mail configuration settings.
   *
   * <p>Defines mail sender display name and other mail-related configuration.
   */
  private Mail mail = new Mail();

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
    // Validate I18n configuration
    validateI18nConfig();
  }

  /**
   * Validates CORS allowed origins' configuration.
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
   * Validates I18n configuration.
   *
   * <p>Ensures that the default locale is included in the supported locales list.
   *
   * @throws IllegalStateException if defaultLocale is not in supportedLocales
   */
  private void validateI18nConfig() {
    String defaultLocale = i18n.getDefaultLocale();
    List<String> supportedLocales = i18n.getSupportedLocales();

    if (defaultLocale == null || defaultLocale.isBlank()) {
      throw new IllegalStateException("app.i18n.default-locale must not be blank.");
    }

    if (supportedLocales == null || supportedLocales.isEmpty()) {
      throw new IllegalStateException("app.i18n.supported-locales must not be empty.");
    }

    if (!supportedLocales.contains(defaultLocale)) {
      throw new IllegalStateException(
          "app.i18n.default-locale ('"
              + defaultLocale
              + "') must be in the supported-locales list: "
              + supportedLocales);
    }
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

    /**
     * Expiration time in minutes for inactive bucket entries.
     *
     * <p>Buckets that have not been accessed within this duration will be automatically evicted
     * from the cache to prevent memory leaks.
     *
     * <p>Default: 10 minutes
     */
    private long expireAfterAccessMinutes = 10;

    // ==================== IP Whitelist / Blacklist ====================

    /**
     * IP whitelist entries (supports CIDR notation and single IP addresses).
     *
     * <p>Whitelisted IPs bypass rate limiting entirely. Supports IPv4/IPv6 CIDR notation (e.g.,
     * {@code 10.0.0.0/8}) and individual IP addresses (e.g., {@code 203.0.113.50}).
     *
     * <p>Default: empty list (no whitelisted IPs)
     */
    private List<String> whiteList = new ArrayList<>();

    /**
     * Auto-ban duration in minutes for IPs that exceed the login failure threshold.
     *
     * <p>When a non-whitelisted IP reaches {@link #ipMaxFailures} failed login attempts within the
     * {@link #ipFailWindowMinutes} window, it is automatically added to the Redis blacklist for
     * this duration.
     *
     * <p>Default: 60 minutes (1 hour)
     */
    private long ipBlockDurationMinutes = 60;

    /**
     * Maximum number of failed login attempts allowed per IP before auto-ban.
     *
     * <p>Only applies to non-whitelisted IPs. When this threshold is reached within the {@link
     * #ipFailWindowMinutes} window, the IP is automatically blacklisted.
     *
     * <p>Default: 20 attempts
     */
    private int ipMaxFailures = 20;

    /**
     * Time window in minutes for counting IP-level login failures.
     *
     * <p>The failure counter for each IP resets after this duration of inactivity.
     *
     * <p>Default: 10 minutes
     */
    private long ipFailWindowMinutes = 10;

    // ==================== Account Lock ====================

    /**
     * Maximum number of failed login attempts allowed per account before locking (threshold X).
     *
     * <p>Applies to all IPs (both whitelisted and non-whitelisted). When this threshold is reached,
     * the account is locked with a duration calculated as: {@code (failureCount -
     * accountMaxFailures) * accountLockStepMinutes}, capped at {@link #accountLockMaxHours}.
     *
     * <p>Default: 5 attempts
     */
    private int accountMaxFailures = 5;

    /**
     * Time window in minutes for counting account-level login failures.
     *
     * <p>The failure counter for each account resets after this duration of inactivity.
     *
     * <p>Default: 10 minutes
     */
    private long accountFailWindowMinutes = 10;

    /**
     * Lock duration step in minutes per excess failure (N).
     *
     * <p>Each additional failure beyond {@link #accountMaxFailures} adds this many minutes to the
     * lock duration. For example, with accountMaxFailures=5 and accountLockStepMinutes=5: 6th
     * failure → 5 min lock, 7th failure → 10 min lock, 8th failure → 15 min lock.
     *
     * <p>Default: 5 minutes
     */
    private long accountLockStepMinutes = 5;

    /**
     * Maximum account lock duration in hours (M).
     *
     * <p>The calculated lock duration will never exceed this value regardless of the number of
     * failed attempts.
     *
     * <p>Default: 24 hours
     */
    private long accountLockMaxHours = 24;

    /**
     * Amount to decrement the account failure counter on successful login.
     *
     * <p>Instead of clearing the counter entirely on success, the counter is reduced by this value
     * (floored at 0). This prevents attackers from using a known-good password to reset the counter
     * while still giving legitimate users some leniency after a successful login.
     *
     * <p>For example, with accountSuccessDecrement=2: a user who failed 4 times and then succeeds
     * will have a counter of 2 afterwards, so one accidental mistype the next day won't trigger a
     * lock.
     *
     * <p>Default: 2
     */
    private long accountSuccessDecrement = 2;
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
     * <p>Default: login, forgot-password, reset-password endpoints
     */
    private String[] publicEndpoints = {
      "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password"
    };

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

    /**
     * Content Security Policy configuration.
     *
     * <p>Defines CSP directives that can be customized per environment to balance security and
     * development convenience.
     */
    private Csp csp = new Csp();
  }

  /**
   * Content Security Policy configuration inner class.
   *
   * <p>Binds properties under the {@code app.security.csp} prefix. Defines CSP directives that can
   * be customized per environment.
   */
  @Data
  public static class Csp {
    /**
     * Script source directives for Content-Security-Policy header.
     *
     * <p>Specifies allowed script sources. In production, should be {@code 'self'} only. In
     * development, may include {@code 'unsafe-inline'} and {@code 'unsafe-eval'} for frameworks
     * that require them.
     *
     * <p>Default: 'self' (production-secure)
     */
    private String scriptSrc = "'self'";

    /**
     * Style source directives for Content-Security-Policy header.
     *
     * <p>Specifies allowed style sources. In production, should be {@code 'self'} only. In
     * development, may include {@code 'unsafe-inline'} for hot-reload and dev tools.
     *
     * <p>Default: 'self' (production-secure)
     */
    private String styleSrc = "'self'";

    /**
     * Whether to enable strict CSP mode (no unsafe-inline or unsafe-eval).
     *
     * <p>When {@code true}, enforces strict CSP policy for production. When {@code false}, allows
     * relaxed policy for development environments.
     *
     * <p>Default: true (production-secure)
     */
    private boolean strict = true;
  }

  /**
   * Async thread pool configuration inner class.
   *
   * <p>Binds properties under the {@code app.async} prefix from configuration files. Controls the
   * thread pool parameters for asynchronous log saving operations.
   */
  @Data
  public static class Async {
    /**
     * Core pool size: number of threads always kept alive.
     *
     * <p>Default: 4
     */
    private int corePoolSize = 4;

    /**
     * Maximum pool size: upper limit of threads under high load.
     *
     * <p>Default: 8
     */
    private int maxPoolSize = 8;

    /**
     * Queue capacity: maximum number of pending tasks.
     *
     * <p>Default: 500
     */
    private int queueCapacity = 500;

    /**
     * Thread name prefix for log output identification.
     *
     * <p>Default: "async-log-"
     */
    private String threadNamePrefix = "async-log-";

    /**
     * Whether to wait for tasks to complete on shutdown.
     *
     * <p>When {@code true}, the executor will wait for queued tasks to finish before shutting down,
     * preventing task loss during application shutdown.
     *
     * <p>Default: true
     */
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * Maximum time (in seconds) to wait for tasks to complete on shutdown.
     *
     * <p>Default: 30 seconds
     */
    private int awaitTerminationSeconds = 30;
  }

  /**
   * Inner class for access log configuration.
   *
   * <p>Binds properties under the {@code app.access-log} prefix. Defines excluded path patterns for
   * the access log interceptor and batch write parameters for buffered persistence.
   */
  @Data
  public static class AccessLog {
    /**
     * URL path patterns to exclude from access log recording.
     *
     * <p>Ant patterns passed to Spring MVC's {@code excludePathPatterns}. Specifies paths that do
     * not require logging, such as static resources, health checks, and error pages.
     *
     * <p>Default: actuator, favicon.ico, error endpoints
     */
    private String[] excludePathPatterns = {"/actuator/**", "/favicon.ico", "/error"};

    /**
     * Maximum number of log entries per batch INSERT.
     *
     * <p>When the buffer reaches this size, a flush is triggered immediately regardless of the
     * scheduled interval. Also used as the drain limit per scheduled flush cycle.
     *
     * <p>Default: 100
     */
    private int batchSize = 100;

    /**
     * Scheduled flush interval in milliseconds.
     *
     * <p>A fixed-rate scheduled task drains the buffer at this interval, ensuring logs are
     * persisted even under low traffic.
     *
     * <p>Default: 5000 (5 seconds)
     */
    private long flushIntervalMs = 5000;

    /**
     * Maximum number of batches to flush in a single scheduled invocation.
     *
     * <p>Prevents a single flush cycle from monopolizing the scheduler thread for too long when the
     * buffer has a massive backlog. Each invocation processes up to {@code maxBatchesPerFlush *
     * batchSize} entries.
     *
     * <p>Default: 20
     */
    private int maxBatchesPerFlush = 20;
  }

  /**
   * I18n internationalization configuration inner class.
   *
   * <p>Binds properties under the {@code app.i18n} prefix from configuration files. Defines default
   * locale and supported locales for internationalization.
   */
  @Data
  public static class I18n {
    /**
     * Default locale for the application.
     *
     * <p>This locale is used when no locale is specified by the client. Must be included in the
     * {@link #supportedLocales} list.
     *
     * <p>Default: "en"
     */
    private String defaultLocale = "en";

    /**
     * List of supported locales for internationalization.
     *
     * <p>These locales are available for clients to use. The {@link #defaultLocale} must be
     * included in this list.
     *
     * <p>Default: ["en", "zh", "ja"]
     */
    private List<String> supportedLocales = List.of("en", "zh", "ja");
  }

  /**
   * Report configuration inner class.
   *
   * <p>Binds properties under the {@code app.report} prefix. Defines font paths and export limits
   * for PDF report generation.
   */
  @Data
  public static class Report {
    /**
     * Comma-separated list of font file paths for PDF generation.
     *
     * <p>Supports both classpath resources (e.g., {@code classpath:/fonts/}) and filesystem paths
     * (e.g., {@code /opt/fonts/}). All {@code .ttf} and {@code .otf} files in the specified
     * classpath directory will be auto-discovered and registered.
     *
     * <p>For CJ (Chinese/Japanese) support, download Noto Sans fonts and place them in the font
     * directory. Recommended fonts:
     *
     * <ul>
     *   <li>NotoSansSC-Regular.ttf (Simplified Chinese + Latin)
     *   <li>NotoSansSC-Bold.ttf (Simplified Chinese Bold)
     *   <li>NotoSansJP-Regular.ttf (Japanese + Latin)
     *   <li>NotoSansJP-Bold.ttf (Japanese Bold)
     * </ul>
     *
     * <p>Download from: https://fonts.google.com/noto
     *
     * <p>Default: {@code classpath:/fonts/}
     */
    private String fontPath = "classpath:/fonts/";

    /**
     * Maximum number of records allowed in a single PDF report export.
     *
     * <p>Prevents excessive memory usage and long generation times for very large datasets. When
     * the query result exceeds this limit, only the first N records are included in the report.
     *
     * <p>Default: 10000
     */
    private int maxExportRecords = 10000;
  }

  /**
   * Password reset configuration inner class.
   *
   * <p>Binds properties under the {@code app.password-reset} prefix. Defines token TTL and frontend
   * base URL for generating reset password links.
   */
  @Data
  public static class PasswordReset {
    /**
     * Password reset token TTL in minutes.
     *
     * <p>Default: 5 minutes
     */
    private long tokenTtlMinutes = 5;

    /**
     * Frontend base URL for generating reset password links.
     *
     * <p>Default: http://localhost:5173
     */
    private String baseUrl = "http://localhost:5173";
  }

  /**
   * Mail configuration inner class.
   *
   * <p>Binds properties under the {@code app.mail} prefix. Defines mail sender display name.
   */
  @Data
  public static class Mail {
    /** Sender display name for outgoing emails. Default: System Notification */
    private String senderName = "System Notification";
  }
}
