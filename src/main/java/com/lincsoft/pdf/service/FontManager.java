package com.lincsoft.pdf.service;

import com.lincsoft.exception.PdfFontException;
import com.lincsoft.pdf.config.PdfTemplateConfig;
import com.lowagie.text.pdf.BaseFont;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * 字体管理器服务。
 *
 * <p>负责加载和管理 PDF 生成所需的多语言字体，支持中文、英文、日文字体。 实现字体加载失败的降级策略，确保 PDF 生成功能在字体问题下仍能正常工作。
 *
 * <p>该服务实现以下需求：
 *
 * <ul>
 *   <li>Requirement 8.1: 使用支持 GB2312 字符集的中文字体
 *   <li>Requirement 8.2: 支持从配置的外部路径加载中文字体
 *   <li>Requirement 8.3: 未配置外部字体时使用内嵌的默认中文字体
 *   <li>Requirement 8.4: 应用启动时验证内嵌字体文件存在，不存在则抛异常阻止启动
 *   <li>Requirement 8.5: PDF 生成时字体加载失败记录警告并使用备用字体
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Service
@Slf4j
public class FontManager {

  /** 内嵌中文字体路径（classpath 路径） */
  private static final String EMBEDDED_CHINESE_FONT_PATH = "fonts/NotoSansSC-Regular.ttf";

  /** 内嵌日文字体路径（classpath 路径） */
  private static final String EMBEDDED_JAPANESE_FONT_PATH = "fonts/NotoSansJP-Regular.ttf";

  /** 支持中文的语言代码集合 */
  private static final Set<String> CHINESE_LOCALES = Set.of("zh", "zh-cn", "zh-tw", "zh-hk");

  /** 支持日文的语言代码集合 */
  private static final Set<String> JAPANESE_LOCALES = Set.of("ja", "ja-jp");

  /** PDF 模板配置 */
  private final PdfTemplateConfig templateConfig;

  /** 默认字体（用于英文和其他语言） */
  private BaseFont defaultFont;

  /** 中文字体 */
  private BaseFont chineseFont;

  /** 日文字体 */
  private BaseFont japaneseFont;

  /** 内嵌中文字体是否可用 */
  private boolean embeddedChineseFontAvailable = false;

  /** 内嵌日文字体是否可用 */
  private boolean embeddedJapaneseFontAvailable = false;

  /**
   * 构造字体管理器。
   *
   * @param templateConfig PDF 模板配置
   */
  public FontManager(PdfTemplateConfig templateConfig) {
    this.templateConfig = templateConfig;
  }

  /**
   * 应用启动时初始化字体。
   *
   * <p>验证内嵌字体文件是否存在，如果不存在则抛出异常阻止应用启动。 这是 Requirement 8.4 的核心实现。
   *
   * @throws PdfFontException 如果内嵌字体文件不存在
   */
  @PostConstruct
  public void init() {
    log.info("Initializing FontManager...");

    // 初始化默认字体（使用 OpenPDF 内置字体）
    initDefaultFont();

    // 验证并初始化内嵌中文字体
    initEmbeddedChineseFont();

    // 验证并初始化内嵌日文字体
    initEmbeddedJapaneseFont();

    log.info(
        "FontManager initialized. Chinese font available: {}, Japanese font available: {}",
        embeddedChineseFontAvailable,
        embeddedJapaneseFontAvailable);
  }

  /**
   * 获取指定语言的字体。
   *
   * <p>根据语言返回对应的字体：
   *
   * <ul>
   *   <li>中文（zh、zh-CN、zh-TW、zh-HK）-> 中文字体
   *   <li>日文（ja、ja-JP）-> 日文字体
   *   <li>其他 -> 默认字体
   * </ul>
   *
   * <p>如果字体加载失败，会记录警告并使用备用字体。这是 Requirement 8.5 的核心实现。
   *
   * @param locale 语言设置
   * @return 对应的 BaseFont，如果加载失败则返回备用字体
   */
  public BaseFont getFont(Locale locale) {
    if (locale == null) {
      log.debug("Locale is null, using default font");
      return getDefaultFontSafe();
    }

    String language = locale.toLanguageTag().toLowerCase();
    log.debug("Getting font for locale: {}", language);

    // 中文语言使用中文字体
    if (isChineseLocale(language)) {
      return getChineseFontSafe();
    }

    // 日文语言使用日文字体
    if (isJapaneseLocale(language)) {
      return getJapaneseFontSafe();
    }

    // 其他语言使用默认字体
    return getDefaultFontSafe();
  }

  /**
   * 检查中文字体是否可用。
   *
   * @return 如果中文字体可用则返回 true
   */
  public boolean isChineseFontAvailable() {
    return chineseFont != null || embeddedChineseFontAvailable;
  }

  /**
   * 检查日文字体是否可用。
   *
   * @return 如果日文字体可用则返回 true
   */
  public boolean isJapaneseFontAvailable() {
    return japaneseFont != null || embeddedJapaneseFontAvailable;
  }

  /**
   * 初始化默认字体。
   *
   * <p>使用 OpenPDF 内置的基础字体作为默认字体和备用字体。
   */
  private void initDefaultFont() {
    try {
      // 使用 OpenPDF 内置的基础字体作为默认字体
      defaultFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
      log.debug("Default font initialized successfully");
    } catch (IOException | RuntimeException e) {
      // 这种情况理论上不应该发生，因为使用的是内置字体
      log.error("Failed to initialize default font", e);
      throw new PdfFontException("Failed to initialize default font: " + e.getMessage(), e);
    }
  }

  /**
   * 初始化内嵌中文字体。
   *
   * <p>验证内嵌中文字体文件是否存在。如果不存在，抛出异常阻止应用启动。 这是 Requirement 8.4 的实现。
   *
   * @throws PdfFontException 如果内嵌中文字体文件不存在
   */
  private void initEmbeddedChineseFont() {
    // 首先尝试从外部配置路径加载
    String externalPath = templateConfig.getFont().getPaths().getChinese();
    if (externalPath != null && !externalPath.isBlank()) {
      log.info("Attempting to load Chinese font from external path: {}", externalPath);
      try {
        chineseFont = BaseFont.createFont(externalPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        log.info("Chinese font loaded successfully from external path: {}", externalPath);
        return;
      } catch (IOException | RuntimeException e) {
        log.warn(
            "Failed to load Chinese font from external path: {}, will try embedded font",
            externalPath,
            e);
      }
    }

    // 验证内嵌字体是否存在
    ClassPathResource resource = new ClassPathResource(EMBEDDED_CHINESE_FONT_PATH);
    if (!resource.exists()) {
      String message =
          String.format(
              "Embedded Chinese font file not found: %s. Please ensure the font file exists in the"
                  + " classpath or configure an external font path via the"
                  + " 'pdf.template.font.paths.chinese' property.",
              EMBEDDED_CHINESE_FONT_PATH);
      log.error(message);
      throw new PdfFontException(message);
    }

    // 标记内嵌字体可用
    embeddedChineseFontAvailable = true;
    log.info("Embedded Chinese font file verified: {}", EMBEDDED_CHINESE_FONT_PATH);

    // 验证字体文件可读
    try {
      resource.getInputStream().close();
      log.debug("Embedded Chinese font is available for loading");
    } catch (IOException e) {
      log.warn(
          "Embedded Chinese font file exists but cannot be read: {}",
          EMBEDDED_CHINESE_FONT_PATH,
          e);
      embeddedChineseFontAvailable = false;
    }
  }

  /**
   * 初始化内嵌日文字体。
   *
   * <p>与中文字体不同，日文字体不是必需的。如果内嵌日文字体文件不存在，只记录警告但不阻止应用启动。
   */
  private void initEmbeddedJapaneseFont() {
    // 首先尝试从外部配置路径加载
    String externalPath = templateConfig.getFont().getPaths().getJapanese();
    if (externalPath != null && !externalPath.isBlank()) {
      log.info("Attempting to load Japanese font from external path: {}", externalPath);
      try {
        japaneseFont = BaseFont.createFont(externalPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        log.info("Japanese font loaded successfully from external path: {}", externalPath);
        return;
      } catch (IOException | RuntimeException e) {
        log.warn(
            "Failed to load Japanese font from external path: {}, will try embedded font",
            externalPath,
            e);
      }
    }

    // 检查内嵌字体是否存在
    ClassPathResource resource = new ClassPathResource(EMBEDDED_JAPANESE_FONT_PATH);
    if (!resource.exists()) {
      log.warn(
          "Embedded Japanese font file not found: {}. Japanese PDF generation may use fallback"
              + " font.",
          EMBEDDED_JAPANESE_FONT_PATH);
      embeddedJapaneseFontAvailable = false;
      return;
    }

    // 标记内嵌字体可用
    embeddedJapaneseFontAvailable = true;
    log.info("Embedded Japanese font file verified: {}", EMBEDDED_JAPANESE_FONT_PATH);

    // 验证字体文件可读
    try {
      resource.getInputStream().close();
      log.debug("Embedded Japanese font is available for loading");
    } catch (IOException e) {
      log.warn(
          "Embedded Japanese font file exists but cannot be read: {}",
          EMBEDDED_JAPANESE_FONT_PATH,
          e);
      embeddedJapaneseFontAvailable = false;
    }
  }

  /**
   * 安全获取中文字体。
   *
   * <p>如果字体加载失败，记录警告并返回备用字体。这是 Requirement 8.5 的实现。
   *
   * @return 中文字体或备用字体
   */
  private BaseFont getChineseFontSafe() {
    // 如果已经加载，直接返回
    if (chineseFont != null) {
      return chineseFont;
    }

    // 尝试加载字体
    try {
      // 先尝试外部路径
      String externalPath = templateConfig.getFont().getPaths().getChinese();
      if (externalPath != null && !externalPath.isBlank()) {
        chineseFont = BaseFont.createFont(externalPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return chineseFont;
      }

      // 使用内嵌字体
      if (embeddedChineseFontAvailable) {
        ClassPathResource resource = new ClassPathResource(EMBEDDED_CHINESE_FONT_PATH);
        try (InputStream is = resource.getInputStream()) {
          byte[] fontBytes = is.readAllBytes();
          chineseFont =
              BaseFont.createFont(
                  EMBEDDED_CHINESE_FONT_PATH,
                  BaseFont.IDENTITY_H,
                  BaseFont.EMBEDDED,
                  false,
                  fontBytes,
                  null);
          return chineseFont;
        }
      }
    } catch (IOException | RuntimeException e) {
      log.warn("Failed to load Chinese font, using fallback font. Error: {}", e.getMessage());
    }

    // 返回备用字体
    return getFallbackFont("Chinese");
  }

  /**
   * 安全获取日文字体。
   *
   * <p>如果字体加载失败，记录警告并返回备用字体。这是 Requirement 8.5 的实现。
   *
   * @return 日文字体或备用字体
   */
  private BaseFont getJapaneseFontSafe() {
    // 如果已经加载，直接返回
    if (japaneseFont != null) {
      return japaneseFont;
    }

    // 尝试加载字体
    try {
      // 先尝试外部路径
      String externalPath = templateConfig.getFont().getPaths().getJapanese();
      if (externalPath != null && !externalPath.isBlank()) {
        japaneseFont = BaseFont.createFont(externalPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return japaneseFont;
      }

      // 使用内嵌字体
      if (embeddedJapaneseFontAvailable) {
        ClassPathResource resource = new ClassPathResource(EMBEDDED_JAPANESE_FONT_PATH);
        try (InputStream is = resource.getInputStream()) {
          byte[] fontBytes = is.readAllBytes();
          japaneseFont =
              BaseFont.createFont(
                  EMBEDDED_JAPANESE_FONT_PATH,
                  BaseFont.IDENTITY_H,
                  BaseFont.EMBEDDED,
                  false,
                  fontBytes,
                  null);
          return japaneseFont;
        }
      }
    } catch (IOException | RuntimeException e) {
      log.warn("Failed to load Japanese font, using fallback font. Error: {}", e.getMessage());
    }

    // 返回备用字体
    return getFallbackFont("Japanese");
  }

  /**
   * 安全获取默认字体。
   *
   * @return 默认字体
   */
  private BaseFont getDefaultFontSafe() {
    if (defaultFont != null) {
      return defaultFont;
    }
    // 如果默认字体也未初始化（不应该发生），尝试重新创建
    try {
      defaultFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
      return defaultFont;
    } catch (IOException | RuntimeException e) {
      // 最后的备用方案：返回 null，让 OpenPDF 使用其默认字体
      log.error("Failed to create default font", e);
      return null;
    }
  }

  /**
   * 获取备用字体。
   *
   * <p>当中文或日文字体加载失败时，尝试使用另一种字体作为备用。 如果两种字体都不可用，则使用默认字体。
   *
   * @param languageName 语言名称（用于日志）
   * @return 备用字体
   */
  private BaseFont getFallbackFont(String languageName) {
    log.warn("{} font not available, attempting to use fallback font", languageName);

    // 如果是中文，尝试使用日文字体作为备用
    if ("Chinese".equals(languageName) && japaneseFont != null) {
      log.info("Using Japanese font as fallback for Chinese");
      return japaneseFont;
    }

    // 如果是日文，尝试使用中文字体作为备用
    if ("Japanese".equals(languageName) && chineseFont != null) {
      log.info("Using Chinese font as fallback for Japanese");
      return chineseFont;
    }

    // 使用默认字体作为最后的备用
    log.warn("Using default font as fallback for {}", languageName);
    return getDefaultFontSafe();
  }

  /**
   * 检查是否为中文语言环境。
   *
   * @param language 语言标签（小写）
   * @return 如果是中文则返回 true
   */
  private boolean isChineseLocale(String language) {
    // 检查精确匹配
    if (CHINESE_LOCALES.contains(language)) {
      return true;
    }
    // 检查前缀匹配（如 zh-cn, zh-tw 等）
    return language.startsWith("zh-");
  }

  /**
   * 检查是否为日文语言环境。
   *
   * @param language 语言标签（小写）
   * @return 如果是日文则返回 true
   */
  private boolean isJapaneseLocale(String language) {
    // 检查精确匹配
    if (JAPANESE_LOCALES.contains(language)) {
      return true;
    }
    // 检查前缀匹配（如 ja-jp 等）
    return language.startsWith("ja-");
  }
}
