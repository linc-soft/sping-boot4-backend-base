package com.lincsoft.pdf.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PDF 模板配置类。
 *
 * <p>定义 PDF 文档的页面布局和样式配置，包括页面尺寸、字体、表格、页眉和页脚等设置。 所有配置项都支持通过 application.yml 中的 {@code pdf.template}
 * 前缀进行覆盖。
 *
 * <p>配置示例：
 *
 * <pre>{@code
 * pdf:
 *   template:
 *     page:
 *       width: 210
 *       height: 297
 *       margin-left: 20
 *     font:
 *       default-size: 10
 *       title-size: 16
 * }</pre>
 *
 * <p>该配置类实现了以下需求：
 *
 * <ul>
 *   <li>Requirement 5.1: 支持配置打印位置（X、Y坐标）
 *   <li>Requirement 5.2: 支持配置打印区域
 *   <li>Requirement 5.3: 支持从预定义的支持字体列表中选择配置字体类型
 *   <li>Requirement 5.4: 支持配置字体大小（6pt 至 72pt）
 *   <li>Requirement 5.5: 支持配置页边距
 *   <li>Requirement 5.6: 支持配置列宽
 *   <li>Requirement 5.7: 支持配置表格边框样式
 *   <li>Requirement 5.8: 支持配置页眉页脚
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-07
 * @see PageConfig
 * @see FontConfig
 * @see TableConfig
 * @see HeaderConfig
 * @see FooterConfig
 */
@Data
@Component
@ConfigurationProperties(prefix = "pdf.template")
public class PdfTemplateConfig {

  /** 页面配置 */
  private PageConfig page = new PageConfig();

  /** 字体配置 */
  private FontConfig font = new FontConfig();

  /** 表格配置 */
  private TableConfig table = new TableConfig();

  /** 页眉配置 */
  private HeaderConfig header = new HeaderConfig();

  /** 页脚配置 */
  private FooterConfig footer = new FooterConfig();

  /**
   * 页面配置内部类。
   *
   * <p>定义 PDF 页面的基本尺寸和边距设置。所有尺寸单位均为毫米（mm）。
   *
   * <p>默认值为 A4 纸张尺寸（210mm × 297mm）。
   */
  @Data
  public static class PageConfig {
    /** 页面宽度 (mm)，默认 A4 宽度 210mm */
    private float width = 210f;

    /** 页面高度 (mm)，默认 A4 高度 297mm */
    private float height = 297f;

    /** 左边距 (mm)，默认 20mm */
    private float marginLeft = 20f;

    /** 右边距 (mm)，默认 20mm */
    private float marginRight = 20f;

    /** 上边距 (mm)，默认 25mm */
    private float marginTop = 25f;

    /** 下边距 (mm)，默认 25mm */
    private float marginBottom = 25f;
  }

  /**
   * 字体配置内部类。
   *
   * <p>定义 PDF 文档的字体设置，包括字体路径和各级字体大小。
   */
  @Data
  public static class FontConfig {
    /** 字体路径配置 */
    private FontPaths paths = new FontPaths();

    /** 默认字体大小 (pt)，默认 10pt */
    private float defaultSize = 10f;

    /** 标题字体大小 (pt)，默认 16pt */
    private float titleSize = 16f;

    /** 表头字体大小 (pt)，默认 10pt */
    private float headerSize = 10f;
  }

  /**
   * 字体路径配置内部类。
   *
   * <p>定义外部字体文件的路径，支持中文和日文字体的自定义配置。
   */
  @Data
  public static class FontPaths {
    /** 中文字体路径 */
    private String chinese;

    /** 日文字体路径 */
    private String japanese;
  }

  /**
   * 表格配置内部类。
   *
   * <p>定义 PDF 表格的样式设置，包括边框和列宽配置。
   */
  @Data
  public static class TableConfig {
    /** 边框宽度 (pt)，默认 0.5pt */
    private float borderWidth = 0.5f;

    /** 边框线型，默认实线 */
    private BorderStyle borderStyle = BorderStyle.SOLID;

    /** 列宽配置，键为列名，值为宽度比例 */
    private Map<String, Float> columnWidths = new HashMap<>();
  }

  /**
   * 页眉配置内部类。
   *
   * <p>定义 PDF 文档页眉的样式和内容设置。
   */
  @Data
  public static class HeaderConfig {
    /** 页眉高度 (mm)，默认 15mm */
    private float height = 15f;

    /** 页眉内容 */
    private String content;

    /** 垂直位置，默认居中 */
    private VerticalAlignment verticalAlignment = VerticalAlignment.CENTER;
  }

  /**
   * 页脚配置内部类。
   *
   * <p>定义 PDF 文档页脚的样式和页码设置。
   */
  @Data
  public static class FooterConfig {
    /** 页脚高度 (mm)，默认 15mm */
    private float height = 15f;

    /** 是否显示页码，默认显示 */
    private boolean showPageNumber = true;

    /** 页码格式，支持 {current} 和 {total} 占位符，默认 "当前页/总页数" */
    private String pageFormat = "{current}/{total}";
  }
}
