package com.lincsoft.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Internationalization configuration category.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Configuration
@RequiredArgsConstructor
public class I18nConfig {

  private final AppProperties appProperties;

  @Bean
  public MessageSource messageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasenames("i18n/messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setDefaultLocale(Locale.of(appProperties.getI18n().getDefaultLocale()));
    messageSource.setUseCodeAsDefaultMessage(true);
    return messageSource;
  }
}
