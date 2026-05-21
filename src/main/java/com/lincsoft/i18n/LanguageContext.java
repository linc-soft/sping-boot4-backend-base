package com.lincsoft.i18n;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language context, implementing request level language isolation based on ThreadLocal.
 *
 * <p>Provide read and write access capabilities for the current request language. Return the
 * default language en when called by a non requesting thread.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public final class LanguageContext {

  private static final Logger log = LoggerFactory.getLogger(LanguageContext.class);

  /** Default language */
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

  /** ThreadLocal stores the Locale of the current thread */
  private static final ThreadLocal<Locale> LOCALE_HOLDER = new ThreadLocal<>();

  private LanguageContext() {
    // Private constructor to prevent instantiation
  }

  /**
   * Retrieve the language settings of the current thread.
   *
   * @return Current language, if not set, return to default language en
   */
  public static Locale getLocale() {
    Locale locale = LOCALE_HOLDER.get();
    if (locale == null) {
      log.debug("LanguageContext not set, returning default locale: {}", DEFAULT_LOCALE);
      return DEFAULT_LOCALE;
    }
    return locale;
  }

  /**
   * Set the language of the current thread.
   *
   * @param locale Language to be set
   */
  public static void setLocale(Locale locale) {
    if (locale == null) {
      log.warn("Attempting to set null locale, ignoring");
      return;
    }
    LOCALE_HOLDER.set(locale);
    log.debug("LanguageContext set to: {}", locale);
  }

  /**
   * Clear the language settings of the current thread.
   *
   * <p>It must be called after the request processing is completed to prevent memory leakage.
   */
  public static void clear() {
    LOCALE_HOLDER.remove();
    log.debug("LanguageContext cleared");
  }

  /**
   * Get the string representation of the current language.
   *
   * @return Language code strings, such as "en", "zh", "ja"
   */
  public static String getLanguage() {
    return getLocale().getLanguage();
  }
}
