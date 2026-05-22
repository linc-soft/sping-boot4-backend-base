package com.lincsoft.pdf.service;

import com.lincsoft.i18n.MessageService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

/**
 * PDF 消息服务，包装现有 MessageService，添加翻译缺失时的处理逻辑。
 *
 * <p>当翻译缺失时，返回 key 作为默认值，确保 PDF 生成流程不会因翻译缺失而中断。
 *
 * <p>此服务专门为 PDF 生成模块设计，提供以下功能：
 *
 * <ul>
 *   <li>包装 MessageService 获取国际化消息
 *   <li>捕获 NoSuchMessageException 并返回 key 作为默认值
 *   <li>记录翻译缺失的警告日志
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 * @see MessageService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfMessageService {

  private final MessageService messageService;

  /**
   * 获取消息（无参数）。
   *
   * <p>使用当前线程的语言上下文获取消息。
   *
   * @param key 消息键
   * @return 解析后的消息文本，如果翻译缺失则返回 key 本身
   */
  public String getMessage(String key) {
    return getMessage(key, (Object[]) null);
  }

  /**
   * 获取消息（带格式化参数）。
   *
   * <p>使用当前线程的语言上下文获取消息并进行格式化。
   *
   * @param key 消息键
   * @param args 格式化参数
   * @return 解析后的消息文本，如果翻译缺失则返回 key 本身
   */
  public String getMessage(String key, Object... args) {
    Locale locale = com.lincsoft.i18n.LanguageContext.getLocale();
    return getMessage(key, locale, args);
  }

  /**
   * 获取消息（指定语言，带格式化参数）。
   *
   * <p>这是核心方法，包装 MessageService 并处理翻译缺失的情况。
   *
   * <p>处理逻辑：
   *
   * <ul>
   *   <li>调用 MessageService 获取国际化消息
   *   <li>如果捕获 NoSuchMessageException，记录警告日志并返回 key 作为默认值
   *   <li>如果参数格式错误，尝试返回无参数版本的消息
   * </ul>
   *
   * @param key 消息键
   * @param locale 目标语言
   * @param args 格式化参数
   * @return 解析后的消息文本，如果翻译缺失则返回 key 本身
   */
  public String getMessage(String key, Locale locale, Object... args) {
    try {
      // 确保 args 不为 null，以避免 varargs 匹配问题
      Object[] safeArgs = args != null ? args : new Object[0];
      return messageService.getMessage(key, locale, safeArgs);
    } catch (NoSuchMessageException e) {
      log.warn("Translation missing for key: {}, locale: {}, using key as fallback", key, locale);
      // 返回 key 作为默认值 (Requirement 6.7)
      return key;
    } catch (IllegalArgumentException e) {
      log.warn(
          "Message format error for key: {}, locale: {}, args: {}, trying without args",
          key,
          locale,
          args);
      try {
        return messageService.getMessage(key, locale, new Object[0]);
      } catch (NoSuchMessageException ex) {
        // 再次捕获，返回 key
        return key;
      }
    }
  }
}
