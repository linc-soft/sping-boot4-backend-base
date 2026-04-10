package com.lincsoft.config;

import com.lincsoft.interceptor.AccessLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration class.
 *
 * <p>Customizes Spring MVC settings, such as interceptor registration.
 *
 * @author LINC Technology
 * @since 2026-04-10
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  /** Access log interceptor */
  private final AccessLogInterceptor accessLogInterceptor;

  /**
   * Registers interceptors.
   *
   * <p>Applies the access log interceptor to all URL paths.
   *
   * @param registry the interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(accessLogInterceptor).addPathPatterns("/**");
  }
}
