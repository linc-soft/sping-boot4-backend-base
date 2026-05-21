package com.lincsoft.i18n;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Language interceptors manage the lifecycle of LanguageContext.
 *
 * <p>Resolve Accept Language and set LanguageContext in the preHandle stage, and clear
 * LanguageContext in the afterCompletion stage.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Slf4j
@Component
public class LocaleInterceptor implements HandlerInterceptor {

  private final LocaleResolver localeResolver;

  public LocaleInterceptor(@Lazy LocaleResolver localeResolver) {
    this.localeResolver = localeResolver;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {

    try {
      Locale locale = localeResolver.resolveLocale(request);
      LanguageContext.setLocale(locale);
      log.debug("Locale set to: {}", locale);
    } catch (Exception e) {
      log.warn("Failed to resolve locale, using default: {}", e.getMessage());
      LanguageContext.setLocale(Locale.ENGLISH);
    }

    return true;
  }

  @Override
  public void afterCompletion(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler,
      Exception ex) {

    LanguageContext.clear();
    log.debug("LanguageContext cleared after request completion");
  }
}
