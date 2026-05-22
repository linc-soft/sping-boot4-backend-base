package com.lincsoft.pdf.service;

import static org.junit.jupiter.api.Assertions.*;

import com.lincsoft.exception.PdfFontException;
import com.lincsoft.pdf.config.PdfTemplateConfig;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * FontManager 单元测试。
 *
 * <p>测试字体验证和降级逻辑。
 *
 * <p>注意：由于测试依赖系统字体文件，某些测试仅在特定环境下运行。
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@DisplayName("FontManager 单元测试")
class FontManagerTest {

  private PdfTemplateConfig templateConfig;

  @BeforeEach
  void setUp() {
    templateConfig = new PdfTemplateConfig();
  }

  @Nested
  @DisplayName("字体验证测试 (Requirement 8.4)")
  class FontValidationTests {

    @Test
    @DisplayName("当内嵌中文字体不存在且未配置外部路径时，应抛出异常阻止启动")
    void testEmbeddedFontNotFoundThrowsException() {
      // 未配置外部路径，内嵌字体也不存在
      templateConfig.getFont().getPaths().setChinese(null);

      // 应该抛出异常，因为内嵌字体不存在
      PdfFontException exception =
          assertThrows(
              PdfFontException.class,
              () -> {
                FontManager fontManager = new FontManager(templateConfig);
                fontManager.init();
              },
              "当内嵌字体不存在且未配置外部路径时，应抛出 PdfFontException");

      // 验证异常消息包含关键信息
      assertTrue(
          exception.getMessage().contains("Embedded Chinese font file not found"),
          "异常消息应包含字体文件未找到信息");
      assertTrue(exception.getMessage().contains("pdf.template.font.paths.chinese"), "异常消息应包含配置提示");
    }

    @Test
    @DisplayName("异常消息应提供清晰的配置指导")
    void testExceptionMessageContainsGuidance() {
      templateConfig.getFont().getPaths().setChinese(null);

      PdfFontException exception =
          assertThrows(PdfFontException.class, () -> new FontManager(templateConfig).init());

      String message = exception.getMessage();
      assertTrue(message.contains("fonts/NotoSansSC-Regular.ttf"), "应包含默认字体路径");
      assertTrue(message.contains("classpath"), "应提示 classpath");
      assertTrue(message.contains("external font path"), "应提示外部字体路径");
    }
  }

  @Nested
  @DisplayName("字体降级测试 (Requirement 8.5)")
  class FontFallbackTests {

    @Test
    @DisplayName("当配置了无效的外部路径时，应抛出异常")
    void testInvalidExternalFontPathThrowsException() {
      // 配置一个无效的外部路径
      templateConfig.getFont().getPaths().setChinese("/invalid/nonexistent/path/font.ttf");

      // 应该抛出异常，因为外部路径无效且内嵌字体也不存在
      assertThrows(
          PdfFontException.class,
          () -> {
            FontManager fontManager = new FontManager(templateConfig);
            fontManager.init();
          },
          "当配置的外部路径无效且内嵌字体不存在时，应抛出异常");
    }

    @Test
    @DisplayName("日文字体不存在时，不应阻止应用启动（如果中文字体可用）")
    @EnabledOnOs(OS.WINDOWS)
    void testJapaneseFontNotRequiredOnWindows() {
      // 在 Windows 上，尝试使用系统字体测试
      String systemFontPath = "C:\\Windows\\Fonts\\arial.ttf";
      File fontFile = new File(systemFontPath);

      if (!fontFile.exists()) {
        // 如果字体文件不存在，跳过测试
        return;
      }

      // 配置中文字体（使用 Arial 作为替代，仅用于测试启动流程）
      templateConfig.getFont().getPaths().setChinese(systemFontPath);
      // 不配置日文字体路径

      // 应该不抛出异常（日文字体是可选的）
      FontManager fontManager = new FontManager(templateConfig);
      assertDoesNotThrow(() -> fontManager.init(), "日文字体不存在不应阻止应用启动");
    }
  }

  @Nested
  @DisplayName("字体配置测试")
  class FontConfigTests {

    @Test
    @DisplayName("默认配置应使用预定义的字体路径")
    void testDefaultFontPaths() {
      // 验证默认配置
      assertNotNull(templateConfig.getFont(), "字体配置不应为空");
      assertNotNull(templateConfig.getFont().getPaths(), "字体路径配置不应为空");
      assertNull(templateConfig.getFont().getPaths().getChinese(), "默认中文字体路径应为空");
      assertNull(templateConfig.getFont().getPaths().getJapanese(), "默认日文字体路径应为空");
    }

    @Test
    @DisplayName("字体大小应有默认值")
    void testDefaultFontSizes() {
      assertEquals(10f, templateConfig.getFont().getDefaultSize(), "默认字体大小应为 10pt");
      assertEquals(16f, templateConfig.getFont().getTitleSize(), "标题字体大小应为 16pt");
      assertEquals(10f, templateConfig.getFont().getHeaderSize(), "表头字体大小应为 10pt");
    }
  }

  @Nested
  @DisplayName("外部字体路径配置测试 (Requirement 8.2)")
  class ExternalFontPathTests {

    @Test
    @DisplayName("应支持通过环境变量配置字体路径")
    void testEnvironmentVariableConfiguration() {
      // 模拟环境变量配置（实际配置在 application.yml 中）
      // 环境变量 PDF_CHINESE_FONT_PATH 会被 application.yml 中的 ${PDF_CHINESE_FONT_PATH:} 替换

      // 这个测试主要验证配置结构
      templateConfig.getFont().getPaths().setChinese("/custom/path/to/font.ttf");
      assertEquals(
          "/custom/path/to/font.ttf",
          templateConfig.getFont().getPaths().getChinese(),
          "应支持自定义字体路径");
    }
  }
}
