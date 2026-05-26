package com.lincsoft.i18n;

import com.lincsoft.constant.MessageEnums;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

/**
 * Message service, providing multilingual message parsing and formatting functions.
 *
 * <p>Retrieve the current language from LanguageContext and call Messages Source to parse the
 * message key.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageSource messageSource;

  /**
   * Get message (without parameters).
   *
   * @param messageKey Message key
   * @return Parsed message text
   */
  public String getMessage(String messageKey) {
    return getMessage(messageKey, (Object[]) null);
  }

  /**
   * Retrieve message (with formatting parameters).
   *
   * <p>Retrieve the current language from LanguageContext for message parsing.
   *
   * @param messageKey Message key
   * @param args Format parameters
   * @return Parsed message text
   */
  public String getMessage(String messageKey, Object... args) {
    Locale locale = LanguageContext.getLocale();
    return getMessage(messageKey, locale, args);
  }

  /**
   * Retrieve message (specifying language and formatting parameters).
   *
   * <p>Handle the following abnormal situations:
   *
   * <ul>
   *   <li>Message key does not exist: return message key itself
   *   <li>Parameter mismatch: Return unformatted message template
   * </ul>
   *
   * @param messageKey Message key
   * @param locale target language
   * @param args Format parameters
   * @return Parsed message text
   */
  public String getMessage(String messageKey, Locale locale, Object... args) {
    try {
      return messageSource.getMessage(messageKey, args, locale);
    } catch (NoSuchMessageException e) {
      log.warn("Message key not found: {}, locale: {}", messageKey, locale);
      return messageKey;
    } catch (IllegalArgumentException e) {
      log.warn("Message format error for key: {}, args: {}", messageKey, args);
      try {
        return messageSource.getMessage(messageKey, null, locale);
      } catch (NoSuchMessageException ex) {
        return messageKey;
      }
    }
  }

  /**
   * Retrieve the message (based on Messages, no parameters).
   *
   * @param messageEnum Message enumeration
   * @return Parsed message text
   */
  public String getMessage(MessageEnums messageEnum) {
    return getMessage(toMessageKey(messageEnum));
  }

  /**
   * Retrieve the message (based on Messages with formatting parameters).
   *
   * @param messageEnum Message enumeration
   * @param args Format parameters
   * @return Parsed message text
   */
  public String getMessage(MessageEnums messageEnum, Object... args) {
    return getMessage(toMessageKey(messageEnum), args);
  }

  /**
   * Convert Messages to Message Keys.
   *
   * <p>The message key format is error. {enumeration name lowercase underline}. For example:
   * NOT-FOUND ->error. not_found
   *
   * @param messageEnum Message enumeration
   * @return Message key
   */
  private String toMessageKey(MessageEnums messageEnum) {
    String key = messageEnum.getMessageKey();
    if (key != null) {
      return key;
    }
    return "error." + messageEnum.name().toLowerCase();
  }
}
