package com.lincsoft.pdf.config;

import com.lincsoft.exception.PdfValidationException;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * PDF 模板配置验证器。
 *
 * <p>在应用启动时验证 PDF 模板配置参数是否在有效范围内，并提供运行时验证 API。
 *
 * <p>验证规则：
 *
 * <ul>
 *   <li>页面坐标范围 - X: 0-210mm, Y: 0-297mm (Requirement 5.1)
 *   <li>打印区域范围 - 长度: 1-297mm, 高度: 1-210mm (Requirement 5.2)
 *   <li>字体大小范围 - 6-72pt (Requirement 5.4)
 *   <li>页边距范围 - 0 至页面尺寸 50% (Requirement 5.5)
 *   <li>列宽范围 - 10mm 至可打印区域宽度 (Requirement 5.6)
 *   <li>边框宽度范围 - 0.5-3pt (Requirement 5.7)
 * </ul>
 *
 * <p>配置值超出范围时返回错误消息 (Requirement 5.9)。
 *
 * @author 林创科技
 * @since 2026-05-22
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PdfConfigValidator {

  /** 最大页面宽度 (mm) - A4 横版极限 */
  public static final float MAX_PAGE_WIDTH = 210f;

  /** 最大页面高度 (mm) - A4 纵版极限 */
  public static final float MAX_PAGE_HEIGHT = 297f;

  /** 最小字体大小 (pt) */
  public static final float MIN_FONT_SIZE = 6f;

  /** 最大字体大小 (pt) */
  public static final float MAX_FONT_SIZE = 72f;

  /** 最小列宽 (mm) */
  public static final float MIN_COLUMN_WIDTH = 10f;

  /** 最小边框宽度 (pt) */
  public static final float MIN_BORDER_WIDTH = 0.5f;

  /** 最大边框宽度 (pt) */
  public static final float MAX_BORDER_WIDTH = 3f;

  /** 边距占页面尺寸的最大比例 */
  public static final float MAX_MARGIN_RATIO = 0.5f;

  private final PdfTemplateConfig templateConfig;

  /**
   * 应用启动时验证默认配置。
   *
   * <p>如果配置无效，记录错误日志但不阻止应用启动，因为运行时可以使用配置覆盖。
   */
  @PostConstruct
  public void validateOnStartup() {
    log.info("Validating PDF template configuration on startup...");
    List<String> errors = validate(templateConfig);
    if (!errors.isEmpty()) {
      log.error(
          "PDF template configuration has {} validation error(s):\n - {}",
          errors.size(),
          String.join("\n - ", errors));
      throw new PdfValidationException(
          "Invalid PDF template configuration: " + String.join("; ", errors));
    }
    log.info("PDF template configuration validation passed");
  }

  /**
   * 验证 PDF 模板配置。
   *
   * @param config 待验证的配置
   * @return 错误消息列表，为空表示验证通过
   */
  public List<String> validate(PdfTemplateConfig config) {
    List<String> errors = new ArrayList<>();
    if (config == null) {
      errors.add("PdfTemplateConfig is null");
      return errors;
    }

    validatePage(config.getPage(), errors);
    validateFont(config.getFont(), errors);
    validateTable(config.getTable(), config.getPage(), errors);
    validateHeader(config.getHeader(), errors);
    validateFooter(config.getFooter(), errors);

    return errors;
  }

  /**
   * 验证页面配置。
   *
   * <p>验证页面尺寸和边距，包括：
   *
   * <ul>
   *   <li>页面宽度 (X 坐标范围): 0-210mm
   *   <li>页面高度 (Y 坐标范围): 0-297mm
   *   <li>边距 0 至页面尺寸 50%
   * </ul>
   *
   * @param page 页面配置
   * @param errors 错误消息列表
   */
  private void validatePage(PdfTemplateConfig.PageConfig page, List<String> errors) {
    if (page == null) {
      errors.add("Page config is null");
      return;
    }

    // 页面宽度: 0 < width <= 210mm (Requirement 5.1, 5.2)
    if (page.getWidth() <= 0 || page.getWidth() > MAX_PAGE_WIDTH) {
      errors.add(
          String.format(
              "Page width must be between 1 and %.0fmm, got: %.2f",
              MAX_PAGE_WIDTH, page.getWidth()));
    }

    // 页面高度: 0 < height <= 297mm
    if (page.getHeight() <= 0 || page.getHeight() > MAX_PAGE_HEIGHT) {
      errors.add(
          String.format(
              "Page height must be between 1 and %.0fmm, got: %.2f",
              MAX_PAGE_HEIGHT, page.getHeight()));
    }

    // 边距: 0 至页面尺寸 50% (Requirement 5.5)
    float maxLeftRight = page.getWidth() * MAX_MARGIN_RATIO;
    float maxTopBottom = page.getHeight() * MAX_MARGIN_RATIO;

    if (page.getMarginLeft() < 0 || page.getMarginLeft() > maxLeftRight) {
      errors.add(
          String.format(
              "Margin left must be between 0 and %.2fmm (50%% of page width), got: %.2f",
              maxLeftRight, page.getMarginLeft()));
    }
    if (page.getMarginRight() < 0 || page.getMarginRight() > maxLeftRight) {
      errors.add(
          String.format(
              "Margin right must be between 0 and %.2fmm (50%% of page width), got: %.2f",
              maxLeftRight, page.getMarginRight()));
    }
    if (page.getMarginTop() < 0 || page.getMarginTop() > maxTopBottom) {
      errors.add(
          String.format(
              "Margin top must be between 0 and %.2fmm (50%% of page height), got: %.2f",
              maxTopBottom, page.getMarginTop()));
    }
    if (page.getMarginBottom() < 0 || page.getMarginBottom() > maxTopBottom) {
      errors.add(
          String.format(
              "Margin bottom must be between 0 and %.2fmm (50%% of page height), got: %.2f",
              maxTopBottom, page.getMarginBottom()));
    }
  }

  /**
   * 验证字体配置。
   *
   * <p>验证字体大小是否在 6-72pt 范围内 (Requirement 5.4)。
   *
   * @param font 字体配置
   * @param errors 错误消息列表
   */
  private void validateFont(PdfTemplateConfig.FontConfig font, List<String> errors) {
    if (font == null) {
      errors.add("Font config is null");
      return;
    }

    validateFontSize("default-size", font.getDefaultSize(), errors);
    validateFontSize("title-size", font.getTitleSize(), errors);
    validateFontSize("header-size", font.getHeaderSize(), errors);
  }

  /**
   * 验证单个字体大小。
   *
   * @param name 字体属性名
   * @param size 字体大小
   * @param errors 错误消息列表
   */
  private void validateFontSize(String name, float size, List<String> errors) {
    if (size < MIN_FONT_SIZE || size > MAX_FONT_SIZE) {
      errors.add(
          String.format(
              "Font %s must be between %.0f and %.0fpt, got: %.2f",
              name, MIN_FONT_SIZE, MAX_FONT_SIZE, size));
    }
  }

  /**
   * 验证表格配置。
   *
   * <p>验证边框宽度和列宽。
   *
   * @param table 表格配置
   * @param page 页面配置（用于计算可打印区域）
   * @param errors 错误消息列表
   */
  private void validateTable(
      PdfTemplateConfig.TableConfig table, PdfTemplateConfig.PageConfig page, List<String> errors) {
    if (table == null) {
      errors.add("Table config is null");
      return;
    }

    // 边框宽度: 0.5-3pt (Requirement 5.7)
    if (table.getBorderWidth() < MIN_BORDER_WIDTH || table.getBorderWidth() > MAX_BORDER_WIDTH) {
      errors.add(
          String.format(
              "Table border width must be between %.1f and %.1fpt, got: %.2f",
              MIN_BORDER_WIDTH, MAX_BORDER_WIDTH, table.getBorderWidth()));
    }

    // 列宽: 10mm 至可打印区域宽度 (Requirement 5.6)
    if (table.getColumnWidths() != null && !table.getColumnWidths().isEmpty() && page != null) {
      float printableWidth = page.getWidth() - page.getMarginLeft() - page.getMarginRight();
      table
          .getColumnWidths()
          .forEach(
              (column, width) -> {
                if (width == null) {
                  return;
                }
                if (width < MIN_COLUMN_WIDTH || width > printableWidth) {
                  errors.add(
                      String.format(
                          "Column width for '%s' must be between %.0fmm and %.2fmm, got: %.2f",
                          column, MIN_COLUMN_WIDTH, printableWidth, width));
                }
              });
    }
  }

  /**
   * 验证页眉配置。
   *
   * @param header 页眉配置
   * @param errors 错误消息列表
   */
  private void validateHeader(PdfTemplateConfig.HeaderConfig header, List<String> errors) {
    if (header == null) {
      return;
    }
    if (header.getHeight() < 0 || header.getHeight() > MAX_PAGE_HEIGHT) {
      errors.add(
          String.format(
              "Header height must be between 0 and %.0fmm, got: %.2f",
              MAX_PAGE_HEIGHT, header.getHeight()));
    }
  }

  /**
   * 验证页脚配置。
   *
   * @param footer 页脚配置
   * @param errors 错误消息列表
   */
  private void validateFooter(PdfTemplateConfig.FooterConfig footer, List<String> errors) {
    if (footer == null) {
      return;
    }
    if (footer.getHeight() < 0 || footer.getHeight() > MAX_PAGE_HEIGHT) {
      errors.add(
          String.format(
              "Footer height must be between 0 and %.0fmm, got: %.2f",
              MAX_PAGE_HEIGHT, footer.getHeight()));
    }
  }
}
