package com.lincsoft.config;

import com.lincsoft.interceptor.AccessLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration class.
 *
 * <p>Customizes Spring MVC settings, such as interceptor registration. The access log interceptor's
 * excluded paths can be configured externally via {@code app.access-log.exclude-path-patterns}.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  /** Access log interceptor */
  private final AccessLogInterceptor accessLogInterceptor;

  /** Application configuration properties */
  private final AppProperties appProperties;

  /**
   * Registers interceptors.
   *
   * <p>Applies the access log interceptor to all URL paths, excluding paths specified in {@code
   * app.access-log.exclude-path-patterns}.
   *
   * @param registry the interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(accessLogInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(appProperties.getAccessLog().getExcludePathPatterns());
  }
}
