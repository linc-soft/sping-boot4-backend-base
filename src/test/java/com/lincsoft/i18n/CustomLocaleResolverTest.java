package com.lincsoft.i18n;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lincsoft.config.AppProperties;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 语言解析器单元测试
 *
 * <p>测试 CustomLocaleResolver 的语言解析功能，验证是否符合 PDF 生成的国际化需求。
 *
 * <p>Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("语言解析器测试")
class CustomLocaleResolverTest {

  @Mock private AppProperties appProperties;

  @Mock private AppProperties.I18n i18nConfig;

  private CustomLocaleResolver localeResolver;

  @BeforeEach
  void setUp() {
    // 配置 Mock 对象
    when(appProperties.getI18n()).thenReturn(i18nConfig);
    when(i18nConfig.getDefaultLocale()).thenReturn("en");
    when(i18nConfig.getSupportedLocales()).thenReturn(List.of("en", "zh", "ja"));

    localeResolver = new CustomLocaleResolver(appProperties);
  }

  @Nested
  @DisplayName("精确匹配测试")
  class ExactMatchTests {

    @ParameterizedTest
    @CsvSource({"en, en", "zh, zh", "ja, ja"})
    @DisplayName("应正确解析基础语言代码")
    void testExactMatch_BasicLanguageCode(String acceptLanguage, String expectedLanguage) {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", acceptLanguage);

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals(expectedLanguage, resolved.getLanguage());
    }

    @ParameterizedTest
    @CsvSource({"en-US, en", "en-GB, en", "zh-CN, zh", "ja-JP, ja"})
    @DisplayName("应正确解析带区域的语言代码")
    void testExactMatch_LanguageWithRegion(String acceptLanguage, String expectedLanguage) {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", acceptLanguage);

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals(expectedLanguage, resolved.getLanguage());
    }
  }

  @Nested
  @DisplayName("部分匹配测试")
  class PartialMatchTests {

    @ParameterizedTest
    @CsvSource({"zh-TW, zh", "zh-HK, zh", "zh-SG, zh", "en-AU, en", "en-CA, en", "en-NZ, en"})
    @DisplayName("应正确解析部分匹配的语言代码")
    void testPartialMatch_LanguageVariants(String acceptLanguage, String expectedLanguage) {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", acceptLanguage);

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals(expectedLanguage, resolved.getLanguage());
    }
  }

  @Nested
  @DisplayName("默认语言测试")
  class DefaultLanguageTests {

    @Test
    @DisplayName("Accept-Language 头缺失时应返回默认语言（英文）")
    void testNoAcceptLanguageHeader_ReturnsDefault() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("en", resolved.getLanguage());
    }

    @Test
    @DisplayName("Accept-Language 头为空字符串时应返回默认语言（英文）")
    void testEmptyAcceptLanguageHeader_ReturnsDefault() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("en", resolved.getLanguage());
    }

    @ParameterizedTest
    @CsvSource({"fr, en", "de, en", "es, en", "ko, en", "ar, en", "ru, en"})
    @DisplayName("不支持的语言应回退到默认语言（英文）")
    void testUnsupportedLanguage_ReturnsDefault(String acceptLanguage, String expectedLanguage) {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", acceptLanguage);

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals(expectedLanguage, resolved.getLanguage());
    }
  }

  @Nested
  @DisplayName("权重排序测试")
  class QualityValueTests {

    @Test
    @DisplayName("应按权重选择最高优先级的支持语言")
    void testMultipleLanguages_SelectHighestPriority() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "fr;q=0.9, zh;q=0.8, en;q=0.7");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("zh", resolved.getLanguage());
    }

    @Test
    @DisplayName("当高优先级语言不支持时，应选择次高优先级的支持语言")
    void testMultipleLanguages_UnsupportedHighPriority() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "fr;q=0.9, de;q=0.8, en;q=0.5");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("en", resolved.getLanguage());
    }

    @Test
    @DisplayName("默认权重为 1.0")
    void testDefaultQualityValue() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "zh");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("zh", resolved.getLanguage());
    }
  }

  @Nested
  @DisplayName("边界情况测试")
  class EdgeCaseTests {

    @Test
    @DisplayName("应忽略通配符")
    void testWildcard_ShouldBeIgnored() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "*");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("en", resolved.getLanguage());
    }

    @Test
    @DisplayName("应处理格式错误的语言标签")
    void testMalformedLanguageTag_ReturnsDefault() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "invalid-format-!!!");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      // 格式错误的语言标签应回退到默认语言
      assertEquals("en", resolved.getLanguage());
    }

    @Test
    @DisplayName("应处理空格和大小写")
    void testWhitespaceAndCase() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "  ZH-CN  ");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      assertEquals("zh", resolved.getLanguage());
    }

    @Test
    @DisplayName("应处理多个逗号分隔的语言")
    void testMultipleLanguages_CommaSeparated() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "fr, zh-CN, ja;q=0.8");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      // fr 不支持，zh-CN 支持（默认 q=1.0），ja 支持（q=0.8）
      // 应选择 zh
      assertEquals("zh", resolved.getLanguage());
    }
  }

  @Nested
  @DisplayName("ISO 639-1 标准验证测试")
  class IsoStandardTests {

    @Test
    @DisplayName("应符合 ISO 639-1 标准格式")
    void testIsoStandard() {
      // Given
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("Accept-Language", "zh-CN");

      // When
      Locale resolved = localeResolver.resolveLocale(request);

      // Then
      // 验证返回的 Locale 符合 ISO 639-1 标准
      assertNotNull(resolved.getLanguage());
      assertTrue(resolved.getLanguage().length() == 2 || resolved.getLanguage().isEmpty());
    }
  }
}
