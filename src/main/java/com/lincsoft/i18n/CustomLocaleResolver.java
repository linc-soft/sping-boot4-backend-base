package com.lincsoft.i18n;

import com.lincsoft.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Customize the region parser to parse HTTP Accept Language request headers.
 *
 * <p>Support the following functions:
 *
 * <ul>
 *   <li>Parse individual language code (such as "en", "zh", "ja")
 *   <li>Parse language codes with region subtags (such as "zh-CN", "en-US")
 *   <li>Parse multiple languages and sort them by weight
 *   <li>Handle wildcards and malformed formats
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLocaleResolver extends AcceptHeaderLocaleResolver {

  private final AppProperties appProperties;

  @Override
  @NonNull
  public Locale resolveLocale(@NonNull HttpServletRequest request) {
    String acceptLanguage = request.getHeader("Accept-Language");

    // No Accept Language provided or empty string, return default language
    if (acceptLanguage == null || acceptLanguage.isBlank()) {
      log.debug("No Accept-Language header, using default locale");
      return getConfiguredDefaultLocale();
    }

    // Analyze the Accept Language header
    Locale resolvedLocale = parseAcceptLanguage(acceptLanguage);

    log.debug("Resolved locale: {} from Accept-Language: {}", resolvedLocale, acceptLanguage);
    return resolvedLocale;
  }

  /**
   * Parse Accept Language header, supporting weight sorting and multilingual matching.
   *
   * @param acceptLanguage The value of the Accept Language header
   * @return Parsed Locale
   */
  private Locale parseAcceptLanguage(String acceptLanguage) {
    List<LanguageEntry> entries = parseLanguageEntries(acceptLanguage);

    List<String> supportedLocales = appProperties.getI18n().getSupportedLocales();
    for (LanguageEntry entry : entries) {
      // Skip wildcard characters
      if ("*".equals(entry.language)) {
        continue;
      }

      // Extract main language tags
      String primaryLanguage = extractPrimaryLanguage(entry.language);

      // Match supported language list
      if (supportedLocales.contains(primaryLanguage.toLowerCase())) {
        return Locale.of(primaryLanguage.toLowerCase());
      }
    }

    return getConfiguredDefaultLocale();
  }

  /**
   * Analyze the list of language entries and sort them by weight from high to low.
   *
   * @param acceptLanguage The value of the Accept Language header
   * @return List of language entries sorted by weight
   */
  private List<LanguageEntry> parseLanguageEntries(String acceptLanguage) {
    return Arrays.stream(acceptLanguage.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(this::parseLanguageEntry)
        .filter(e -> e != null)
        .sorted((a, b) -> Double.compare(b.q, a.q))
        .collect(Collectors.toList());
  }

  /**
   * Analyze individual language entries, extract language codes and weight values.
   *
   * @param entry Single language entry, such as "zh CN; q=0.8"
   * @return LanguageEntry object, parsing failed and returned null
   */
  private LanguageEntry parseLanguageEntry(String entry) {
    try {
      String[] parts = entry.split(";");
      String language = parts[0].trim();
      double q = 1.0;

      for (int i = 1; i < parts.length; i++) {
        String param = parts[i].trim();
        if (param.startsWith("q=")) {
          try {
            q = Double.parseDouble(param.substring(2));
          } catch (NumberFormatException e) {
            log.debug("Invalid q value in Accept-Language: {}", param);
          }
        }
      }

      return new LanguageEntry(language.toLowerCase(), q);
    } catch (Exception e) {
      log.debug("Failed to parse language entry: {}", entry);
      return null;
    }
  }

  /**
   * Extract the main language tag from the language tag.
   *
   * <p>For example, "zh CN" is extracted as "zh", and "en US" is extracted as "en".
   *
   * @param languageTag Language tags
   * @return Main language tag
   */
  private String extractPrimaryLanguage(String languageTag) {
    int hyphenIndex = languageTag.indexOf('-');
    if (hyphenIndex > 0) {
      return languageTag.substring(0, hyphenIndex);
    }
    return languageTag;
  }

  /**
   * Get the default language for configuration.
   *
   * @return Default Locale
   */
  private Locale getConfiguredDefaultLocale() {
    String defaultLocale = appProperties.getI18n().getDefaultLocale();
    return Locale.of(defaultLocale);
  }

  /** The language entry internal class stores language code and weight values. */
  private static class LanguageEntry {
    final String language;
    final double q;

    LanguageEntry(String language, double q) {
      this.language = language;
      this.q = q;
    }
  }
}
