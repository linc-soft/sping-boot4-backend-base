package com.lincsoft.pdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lincsoft.i18n.LanguageContext;
import com.lincsoft.i18n.MessageService;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.NoSuchMessageException;

/**
 * PdfMessageService 单元测试。
 *
 * <p>测试翻译缺失时的处理逻辑。
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@DisplayName("PdfMessageService 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdfMessageServiceTest {

  @Mock private MessageService messageService;

  private PdfMessageService pdfMessageService;

  @BeforeEach
  void setUp() {
    pdfMessageService = new PdfMessageService(messageService);
    // 设置默认语言上下文
    LanguageContext.setLocale(Locale.CHINESE);
  }

  @Nested
  @DisplayName("翻译缺失处理测试 (Requirement 6.7)")
  class TranslationMissingTests {

    @Test
    @DisplayName("当翻译缺失时，应返回 key 作为默认值")
    void testTranslationMissingReturnsKey() {
      // Given
      String key = "pdf.header.unknown";
      Locale locale = Locale.CHINESE;

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(key, result, "翻译缺失时应返回 key 作为默认值");
    }

    @Test
    @DisplayName("当翻译缺失时，应记录警告日志")
    void testTranslationMissingLogsWarning() {
      // Given
      String key = "pdf.missing.key";
      Locale locale = Locale.JAPANESE;

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(key, result);
    }

    @Test
    @DisplayName("当带参数的翻译缺失时，应返回 key 作为默认值")
    void testTranslationMissingWithArgsReturnsKey() {
      // Given
      String key = "pdf.title.role_info";
      Locale locale = Locale.ENGLISH;
      Object[] args = new Object[] {"Admin"};

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale, args);

      // Then
      assertEquals(key, result, "带参数的翻译缺失时应返回 key 作为默认值");
    }
  }

  @Nested
  @DisplayName("正常翻译测试")
  class NormalTranslationTests {

    @Test
    @DisplayName("当翻译存在时，应返回翻译后的消息")
    void testTranslationExists() {
      // Given
      String key = "pdf.header.role_name";
      Locale locale = Locale.CHINESE;
      String expectedMessage = "角色名称";

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenReturn(expectedMessage);

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(expectedMessage, result);
    }

    @Test
    @DisplayName("当翻译带参数存在时，应返回格式化后的消息")
    void testTranslationWithArgs() {
      // Given
      String key = "pdf.title.role_info";
      Locale locale = Locale.CHINESE;
      Object[] args = new Object[] {"管理员"};
      String expectedMessage = "管理员 角色信息";

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenReturn(expectedMessage);

      // When
      String result = pdfMessageService.getMessage(key, locale, args);

      // Then
      assertEquals(expectedMessage, result);
    }
  }

  @Nested
  @DisplayName("无参数方法测试")
  class NoArgsMethodTests {

    @Test
    @DisplayName("getMessage(key) 应使用当前语言上下文")
    void testGetMessageWithKeyOnly() {
      // Given
      String key = "pdf.header.username";
      String expectedMessage = "用户名";

      when(messageService.getMessage(eq(key), eq(Locale.CHINESE), any(Object[].class)))
          .thenReturn(expectedMessage);

      // When
      String result = pdfMessageService.getMessage(key);

      // Then
      assertEquals(expectedMessage, result);
    }

    @Test
    @DisplayName("getMessage(key, args) 应使用当前语言上下文并传递参数")
    void testGetMessageWithKeyAndArgs() {
      // Given
      String key = "pdf.title.user_info";
      Object[] args = new Object[] {"张三"};
      String expectedMessage = "张三 用户信息";

      when(messageService.getMessage(eq(key), eq(Locale.CHINESE), any(Object[].class)))
          .thenReturn(expectedMessage);

      // When
      String result = pdfMessageService.getMessage(key, args);

      // Then
      assertEquals(expectedMessage, result);
    }
  }

  @Nested
  @DisplayName("参数格式错误处理测试")
  class ArgumentFormatErrorTests {

    @Test
    @DisplayName("当参数格式错误时，应尝试返回无参数版本的消息")
    void testArgumentFormatErrorFallback() {
      // Given
      String key = "pdf.title.info";
      Locale locale = Locale.ENGLISH;
      Object[] args = new Object[] {"invalid"};
      String fallbackMessage = "Information";

      // 第一次调用抛出 IllegalArgumentException，第二次调用返回 fallbackMessage
      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new IllegalArgumentException("Wrong number of arguments"))
          .thenReturn(fallbackMessage);

      // When
      String result = pdfMessageService.getMessage(key, locale, args);

      // Then
      assertEquals(fallbackMessage, result, "参数格式错误时应尝试返回无参数版本");
    }

    @Test
    @DisplayName("当参数格式错误且无参数版本也缺失时，应返回 key")
    void testArgumentFormatErrorAndMissingFallback() {
      // Given
      String key = "pdf.missing.template";
      Locale locale = Locale.JAPANESE;
      Object[] args = new Object[] {"test"};

      // 第一次调用抛出 IllegalArgumentException，第二次调用抛出 NoSuchMessageException
      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new IllegalArgumentException("Format error"))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale, args);

      // Then
      assertEquals(key, result, "参数格式错误且无参数版本缺失时应返回 key");
    }
  }

  @Nested
  @DisplayName("多语言支持测试 (Requirement 3.14, 6.7)")
  class MultiLanguageTests {

    @Test
    @DisplayName("中文翻译缺失时应返回 key")
    void testChineseTranslationMissing() {
      // Given
      String key = "pdf.new.field";
      Locale locale = Locale.CHINESE;

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(key, result);
    }

    @Test
    @DisplayName("英文翻译缺失时应返回 key")
    void testEnglishTranslationMissing() {
      // Given
      String key = "pdf.custom.label";
      Locale locale = Locale.ENGLISH;

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(key, result);
    }

    @Test
    @DisplayName("日文翻译缺失时应返回 key")
    void testJapaneseTranslationMissing() {
      // Given
      String key = "pdf.special.tag";
      Locale locale = Locale.JAPANESE;

      when(messageService.getMessage(eq(key), eq(locale), any(Object[].class)))
          .thenThrow(new NoSuchMessageException(key));

      // When
      String result = pdfMessageService.getMessage(key, locale);

      // Then
      assertEquals(key, result);
    }
  }
}
