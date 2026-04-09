package com.lincsoft.config;

import com.lincsoft.common.Result;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.filter.JwtAuthorizationFilter;
import com.lincsoft.filter.RateLimitFilter;
import com.lincsoft.services.system.TokenBlacklistService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Spring Security Configuration Class.
 *
 * <p>Security Policies：
 *
 * <ul>
 *   <li>CSRF Protection: CookieCsrfTokenRepository using double cookie validation mode
 *   <li>CORS: withCredentials policy
 *   <li>Session Management: STATELESS mode (server-side session management disabled)
 *   <li>URL Access Rules: Whitelist (login, public API) and authenticated endpoints
 *   <li>Filter Chain: RateLimitFilter, JwtAuthorizationFilter, UsernamePasswordAuthenticationFilter
 *   <li>Password Encoder: BCrypt algorithm
 *   <li>Authentication Manager: Spring Security standard AuthenticationManager
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-09
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  /**
   * User details service used for Spring Security authentication.
   *
   * <p>Loads user-specific data during the authentication process.
   */
  private final UserDetailsService userDetailsService;

  /**
   * Application configuration properties.
   *
   * <p>Used to retrieve CORS and other application-specific settings.
   */
  private final AppProperties appProperties;

  /** Object Mapper for JSON processing. */
  private final ObjectMapper objectMapper;

  /** Token blacklist service for JWT revocation support. */
  private final TokenBlacklistService tokenBlacklistService;

  /**
   * Public endpoints that are exempt from CSRF protection and authentication.
   *
   * <p>These endpoints can be accessed without authentication and do not require CSRF tokens.
   * Typically, includes login endpoints and API documentation.
   */
  private static final String[] PUBLIC_ENDPOINTS = {
    "/api/auth/login", "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
  };

  /**
   * Additional endpoints that do not require authentication but still have CSRF protection enabled.
   *
   * <p>These endpoints are accessible without authentication but still require valid CSRF tokens
   * for state-changing operations.
   */
  private static final String[] AUTH_ONLY_WHITELIST = {
    "/api/auth/refresh", "/error",
  };

  /**
   * Configures the security filter chain.
   *
   * <p>Configuration includes:
   *
   * <ol>
   *   <li>CSRF Protection: Uses CookieCsrfTokenRepository with cookie name "csrfToken" and header
   *       name "X-CSRF-Token". Login endpoints are CSRF exempt, while refresh and logout endpoints
   *       require CSRF tokens.
   *   <li>CORS: Loads allowed origins from configuration file with withCredentials support.
   *   <li>Session Management: STATELESS mode (JWT-based stateless authentication).
   *   <li>URL Access Rules: Login endpoints are permitAll, others require authentication.
   *   <li>Filter Registration: JwtAuthorizationFilter is placed before
   *       UsernamePasswordAuthenticationFilter in the filter chain.
   * </ol>
   *
   * @param http the HttpSecurity builder
   * @return the configured SecurityFilterChain
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    // CSRF Protection: CookieCsrfTokenRepository using double cookie validation mode
    CookieCsrfTokenRepository csrfTokenRepository = getCsrfTokenRepository();

    // CSRF Protection: CsrfTokenRequestAttributeHandler using header only
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    // Disable CSRF token resolution via _csrf parameter (header only)
    requestHandler.setCsrfRequestAttributeName(null);

    http.csrf(
            csrf ->
                csrf
                    // Custom CookieCsrfTokenRepository
                    .csrfTokenRepository(csrfTokenRepository)
                    // Custom CsrfTokenRequestAttributeHandler
                    .csrfTokenRequestHandler(requestHandler)
                    // CSRF Protection: Whitelist (public endpoints)
                    .ignoringRequestMatchers(PUBLIC_ENDPOINTS))
        // CORS Protection: withCredentials policy
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // Session Management: STATELESS mode (server-side session management disabled)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Security Response Headers
        .headers(
            headers ->
                headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
        // URL Access Rules
        .authorizeHttpRequests(
            authorize -> {
              // Public endpoints (CSRF exempt + auth exempt)
              authorize.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
              // Auth-only whitelist (CSRF protected, auth exempt)
              authorize.requestMatchers(AUTH_ONLY_WHITELIST).permitAll();
              // All other requests require authentication
              authorize.anyRequest().authenticated();
            })
        // Filter Chain: RateLimitFilter, JwtAuthorizationFilter,
        // UsernamePasswordAuthenticationFilter
        .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
        // Exception Handling
        // - AuthenticationException: 401 Unauthorized (via unauthorizedEntryPoint)
        // - AccessDeniedException: 403 Forbidden (authenticated but insufficient permissions)
        // Note: Anonymous users triggering AccessDeniedException are automatically
        //       delegated to AuthenticationEntryPoint by ExceptionTranslationFilter.
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(unauthorizedEntryPoint())
                    .accessDeniedHandler(
                        (_, response, _) ->
                            writeSecurityErrorResponse(
                                response, HttpStatus.FORBIDDEN, MessageEnums.FORBIDDEN)));

    return http.build();
  }

  /**
   * Creates and configures a CookieCsrfTokenRepository for CSRF protection.
   *
   * <p>Configuration includes:
   *
   * <ul>
   *   <li>Cookie name and header name from CommonConstants
   *   <li>Cookie path set to root ("/")
   *   <li>SameSite attribute set to "Lax" to mitigate CSRF while allowing top-level navigations
   *   <li>Secure flag controlled by {@code app.csrf.secure} (enabled in production, disabled in
   *       development)
   *   <li>HttpOnly set to false allowing JavaScript access for CSRF token handling
   * </ul>
   *
   * @return a fully configured CookieCsrfTokenRepository instance
   */
  private @NonNull CookieCsrfTokenRepository getCsrfTokenRepository() {
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    // Set CSRF cookie name from constants
    csrfTokenRepository.setCookieName(CommonConstants.CSRF_COOKIE_NAME);
    // Set CSRF header name from constants
    csrfTokenRepository.setHeaderName(CommonConstants.CSRF_HEADER_NAME);
    // Set cookie path to root
    csrfTokenRepository.setCookiePath("/");
    // SameSite=Lax + environment-aware Secure flag
    boolean secure = appProperties.getCsrf().isSecure();
    csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(secure));
    return csrfTokenRepository;
  }

  /**
   * An AuthenticationEntryPoint that returns 401 Unauthorized for unauthenticated requests.
   *
   * @return 401 Unauthorized
   */
  private AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (_, response, _) ->
        writeSecurityErrorResponse(response, HttpStatus.UNAUTHORIZED, MessageEnums.UNAUTHORIZED);
  }

  /**
   * Writes a standardized JSON error response for security exceptions.
   *
   * <p>Used by both {@link #unauthorizedEntryPoint()} and the access denied handler to ensure
   * consistent error response format across all security exception scenarios.
   *
   * @param response the HTTP servlet response to write to
   * @param status the HTTP status code to set (e.g., 401, 403)
   * @param messageEnum the error message enum containing code and message for the response body
   * @throws IOException if an I/O error occurs during response writing
   */
  private void writeSecurityErrorResponse(
      HttpServletResponse response, HttpStatus status, MessageEnums messageEnum)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(Result.error(messageEnum)));
  }

  /**
   * Creates the RateLimitFilter instance.
   *
   * <p>This filter applies per-IP rate limiting using the Bucket4j token-bucket algorithm. It is
   * placed before JwtAuthorizationFilter in the filter chain to reject excessive requests early.
   *
   * @return the configured RateLimitFilter instance
   */
  private RateLimitFilter rateLimitFilter() {
    return new RateLimitFilter(appProperties, objectMapper);
  }

  /**
   * Creates the JwtAuthorizationFilter bean.
   *
   * <p>This filter intercepts incoming requests to validate JWT tokens and set the authentication
   * context for authorized users.
   *
   * @return the configured JwtAuthorizationFilter bean
   */
  private JwtAuthorizationFilter jwtAuthorizationFilter() {
    return new JwtAuthorizationFilter(
        userDetailsService, appProperties, objectMapper, tokenBlacklistService);
  }

  /**
   * CORS Configuration Source.
   *
   * <p>Configuration settings:
   *
   * <ul>
   *   <li>Allowed Origins: {@link AppProperties.Cors#getAllowedOrigins()}
   *   <li>Allowed Methods: GET, POST, PUT, DELETE, OPTIONS
   *   <li>Allowed Headers: Authorization, Content-Type, X-CSRF-TOKEN, X-Trace-Id
   *   <li>Exposed Headers: Authorization, X-Trace-Id
   *   <li>Credentials: true (withCredentials policy)
   *   <li>Max Age: 3600 seconds
   * </ul>
   *
   * @return the configured CorsConfigurationSource bean
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allowed Origins (trim whitespace for robustness)
    List<String> origins =
        Arrays.stream(appProperties.getCors().getAllowedOrigins().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    configuration.setAllowedOrigins(origins);

    // Allowed Methods
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

    // Allowed Headers (explicitly specified to reduce attack surface)
    configuration.setAllowedHeaders(
        Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN", "X-Trace-Id"));

    // Exposed Headers
    configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id"));

    // withCredentials policy: Access-Control-Allow-Credentials: true
    configuration.setAllowCredentials(true);

    // Preflight Cache Time: 3600 seconds
    configuration.setMaxAge(3600L);

    // Apply CORS Configuration to All URL Paths
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }

  /**
   * BCryptPasswordEncoder Bean
   *
   * <p>Hash the password using the BCrypt algorithm. BCrypt is an irreversible hashing algorithm,
   * and since a salt is generated automatically, different hash values are produced even for the
   * same password.
   *
   * @return BCryptPasswordEncoder Bean
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * DaoAuthenticationProvider bean.
   *
   * <p>In Spring Security 7.x, the PasswordEncoder may not be properly injected into
   * DaoAuthenticationProvider during the autoconfiguration of
   * AuthenticationConfiguration.getAuthenticationManager(). Therefore, register a
   * DaoAuthenticationProvider with explicitly configured UserDetailsService and PasswordEncoder as
   * a bean.
   *
   * @return the configured DaoAuthenticationProvider bean
   */
  @Bean
  public DaoAuthenticationProvider daoAuthenticationProvider() {
    // In Spring Security 7.x, use the DaoAuthenticationProvider(UserDetailsService)
    // constructor and set PasswordEncoder separately via setPasswordEncoder()
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  /**
   * AuthenticationManager Bean
   *
   * <p>Obtain the standard {@link AuthenticationManager} from Spring Security's {@link
   * AuthenticationConfiguration}.It is used for username and password authentication in the
   * AuthController.
   *
   * @param config Spring Security AuthenticationConfiguration
   * @return AuthenticationManager Bean
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
    return config.getAuthenticationManager();
  }
}
