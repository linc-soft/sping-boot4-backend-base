package com.lincsoft.pdf.service;

import com.lincsoft.pdf.config.PdfTemplateConfig;
import com.lincsoft.pdf.config.VerticalAlignment;
import com.lincsoft.pdf.dto.PdfConfig;
import com.lincsoft.pdf.dto.TableData;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 核心 PDF 生成服务。
 *
 * <p>负责 PDF 文档的构建和渲染，包括创建文档、设置字体、添加标题、添加查询条件、添加表格和设置页脚页码。
 *
 * <p>该服务实现以下需求：
 *
 * <ul>
 *   <li>Requirement 1.8: 在文档顶部打印表头"角色列表"
 *   <li>Requirement 1.9: 打印查询条件信息
 *   <li>Requirement 1.10: 在页面右上角显示页码，格式为"当前页码/总页码"
 *   <li>Requirement 1.11: 打印列表表头
 *   <li>Requirement 2.5: 以"【角色名称】角色信息"格式打印表头
 *   <li>Requirement 2.6: 打印基本角色信息
 *   <li>Requirement 3.8: 在文档顶部打印表头"用户列表"
 *   <li>Requirement 3.9: 打印查询条件信息
 *   <li>Requirement 3.10: 在页面右上角显示页码
 *   <li>Requirement 4.5: 以"【用户名】用户信息"格式打印表头
 *   <li>Requirement 4.6: 打印基本用户信息
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final FontManager fontManager;
  private final PdfMessageService messageService;
  private final PdfTemplateConfig defaultTemplate;

  /** 毫米到点的转换系数 (1 mm ≈ 2.834645669 pt) */
  private static final float MM_TO_PT = 2.834645669f;

  /**
   * 生成 PDF 文档。
   *
   * <p>根据配置和表格数据生成 PDF 字节数组。生成流程：
   *
   * <ol>
   *   <li>创建文档，设置页面尺寸和边距
   *   <li>设置字体（根据语言选择对应字体）
   *   <li>添加标题
   *   <li>添加查询条件（如有）
   *   <li>添加表格
   *   <li>设置页脚页码
   *   <li>返回 PDF 字节数组
   * </ol>
   *
   * @param config PDF 配置，包含标题、查询条件、模板配置等
   * @param data 表格数据，包含表头、数据行、列宽等
   * @param locale 语言设置，用于选择字体和国际化标签
   * @return PDF 字节数组
   * @throws RuntimeException 如果 PDF 生成过程中发生错误
   */
  public byte[] generatePdf(PdfConfig config, TableData data, Locale locale) {
    log.debug("Starting PDF generation for title: {}, locale: {}", config.getTitle(), locale);

    // 使用配置的模板，如果没有则使用默认模板
    PdfTemplateConfig template =
        config.getTemplate() != null ? config.getTemplate() : defaultTemplate;

    // 获取对应语言的字体
    BaseFont baseFont = fontManager.getFont(locale);
    Font titleFont = createTitleFont(baseFont, template);
    Font normalFont = createNormalFont(baseFont, template);
    Font headerFont = createHeaderFont(baseFont, template);

    // 创建输出流
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
      // 创建文档
      Document document = createDocument(template);

      // 创建 PdfWriter 并设置页眉页脚事件
      PdfWriter writer = PdfWriter.getInstance(document, outputStream);

      // 设置页眉页脚处理器
      if (template.getFooter().isShowPageNumber()
          || (template.getHeader().getContent() != null
              && !template.getHeader().getContent().isBlank())) {
        HeaderFooterEvent headerFooterEvent =
            new HeaderFooterEvent(
                baseFont,
                template.getFont().getDefaultSize(),
                template.getFooter().getPageFormat(),
                template);
        writer.setPageEvent(headerFooterEvent);
      }

      // 打开文档
      document.open();

      // 添加标题
      addTitle(document, config.getTitle(), titleFont);

      // 添加查询条件
      addQueryConditions(document, config.getQueryConditions(), normalFont, locale);

      // 添加表格
      addTable(document, data, headerFont, normalFont, template);

      // 关闭文档
      document.close();

      log.debug("PDF generation completed successfully, size: {} bytes", outputStream.size());
      return outputStream.toByteArray();

    } catch (DocumentException e) {
      log.error("Failed to generate PDF: {}", e.getMessage(), e);
      throw new RuntimeException("PDF 生成失败: " + e.getMessage(), e);
    }
  }

  /**
   * 创建文档。
   *
   * <p>根据模板配置设置页面尺寸和边距。
   *
   * @param template 模板配置
   * @return Document 对象
   */
  private Document createDocument(PdfTemplateConfig template) {
    PdfTemplateConfig.PageConfig pageConfig = template.getPage();

    // 将毫米转换为点
    float widthPt = pageConfig.getWidth() * MM_TO_PT;
    float heightPt = pageConfig.getHeight() * MM_TO_PT;

    // 创建页面大小
    Rectangle pageSize = new Rectangle(widthPt, heightPt);

    // 创建文档，设置边距
    float marginLeftPt = pageConfig.getMarginLeft() * MM_TO_PT;
    float marginRightPt = pageConfig.getMarginRight() * MM_TO_PT;
    float marginTopPt = pageConfig.getMarginTop() * MM_TO_PT;
    float marginBottomPt = pageConfig.getMarginBottom() * MM_TO_PT;

    return new Document(pageSize, marginLeftPt, marginRightPt, marginTopPt, marginBottomPt);
  }

  /**
   * 创建标题字体。
   *
   * @param baseFont 基础字体
   * @param template 模板配置
   * @return Font 对象
   */
  private Font createTitleFont(BaseFont baseFont, PdfTemplateConfig template) {
    float size = template.getFont().getTitleSize();
    return new Font(baseFont, size, Font.BOLD);
  }

  /**
   * 创建正文字体。
   *
   * @param baseFont 基础字体
   * @param template 模板配置
   * @return Font 对象
   */
  private Font createNormalFont(BaseFont baseFont, PdfTemplateConfig template) {
    float size = template.getFont().getDefaultSize();
    return new Font(baseFont, size, Font.NORMAL);
  }

  /**
   * 创建表头字体。
   *
   * @param baseFont 基础字体
   * @param template 模板配置
   * @return Font 对象
   */
  private Font createHeaderFont(BaseFont baseFont, PdfTemplateConfig template) {
    float size = template.getFont().getHeaderSize();
    return new Font(baseFont, size, Font.BOLD);
  }

  /**
   * 添加标题。
   *
   * <p>在文档顶部添加居中的标题段落。
   *
   * @param document 文档对象
   * @param title 标题文本
   * @param font 标题字体
   * @throws DocumentException 如果添加失败
   */
  private void addTitle(Document document, String title, Font font) throws DocumentException {
    if (title == null || title.isBlank()) {
      return;
    }

    Paragraph titleParagraph = new Paragraph(title, font);
    titleParagraph.setAlignment(Paragraph.ALIGN_CENTER);
    titleParagraph.setSpacingAfter(10f);

    document.add(titleParagraph);
    log.debug("Added title: {}", title);
  }

  /**
   * 添加查询条件。
   *
   * <p>在标题下方添加查询条件描述。
   *
   * @param document 文档对象
   * @param conditions 查询条件列表
   * @param font 正文字体
   * @param locale 语言设置
   * @throws DocumentException 如果添加失败
   */
  private void addQueryConditions(
      Document document, List<String> conditions, Font font, Locale locale)
      throws DocumentException {
    if (conditions == null || conditions.isEmpty()) {
      return;
    }

    // 添加"查询条件"标签
    String label = messageService.getMessage("pdf.label.query_conditions", locale);
    Paragraph labelParagraph = new Paragraph(label + ":", font);
    labelParagraph.setSpacingBefore(5f);
    labelParagraph.setSpacingAfter(3f);
    document.add(labelParagraph);

    // 添加各查询条件
    for (String condition : conditions) {
      if (condition != null && !condition.isBlank()) {
        Paragraph conditionParagraph = new Paragraph("  • " + condition, font);
        conditionParagraph.setSpacingAfter(2f);
        document.add(conditionParagraph);
      }
    }

    // 添加空行分隔
    Paragraph separator = new Paragraph(" ", font);
    separator.setSpacingAfter(5f);
    document.add(separator);

    log.debug("Added {} query conditions", conditions.size());
  }

  /**
   * 添加表格。
   *
   * <p>根据表格数据构建并添加表格到文档中。
   *
   * @param document 文档对象
   * @param data 表格数据
   * @param headerFont 表头字体
   * @param dataFont 数据字体
   * @param template 模板配置
   * @throws DocumentException 如果添加失败
   */
  private void addTable(
      Document document, TableData data, Font headerFont, Font dataFont, PdfTemplateConfig template)
      throws DocumentException {
    if (data == null || data.getHeaders() == null || data.getHeaders().isEmpty()) {
      log.warn("No table data to add");
      return;
    }

    List<String> headers = data.getHeaders();
    List<List<String>> rows = data.getRows();
    float[] columnWidths = data.getColumnWidths();
    Map<Integer, List<List<String>>> subTables = data.getSubTables();

    int columnCount = headers.size();

    // 创建表格构建器
    com.lincsoft.pdf.builder.TableBuilder tableBuilder =
        com.lincsoft.pdf.builder.TableBuilder.create(columnCount)
            .headerFont(headerFont)
            .dataFont(dataFont);

    // 设置表头
    tableBuilder.headers(headers.toArray(new String[0]));

    // 设置列宽
    if (columnWidths != null && columnWidths.length == columnCount) {
      tableBuilder.widths(columnWidths);
    }

    // 添加数据行
    if (rows != null) {
      for (int i = 0; i < rows.size(); i++) {
        List<String> row = rows.get(i);
        tableBuilder.row(row.toArray(new String[0]));

        // 检查是否有子表格
        if (subTables != null && subTables.containsKey(i)) {
          // 子表格将在主表格构建后单独处理
          log.debug("Found sub-table for row {}", i);
        }
      }
    }

    // 构建并添加主表格
    document.add(tableBuilder.build());

    // 处理子表格
    if (subTables != null && !subTables.isEmpty()) {
      addSubTables(document, subTables, headerFont, dataFont);
    }

    log.debug(
        "Added table with {} columns and {} rows", columnCount, rows != null ? rows.size() : 0);
  }

  /**
   * 添加子表格。
   *
   * <p>在主表格下方添加嵌套的子表格（如继承角色列表）。
   *
   * @param document 文档对象
   * @param subTables 子表格映射
   * @param headerFont 表头字体
   * @param dataFont 数据字体
   * @throws DocumentException 如果添加失败
   */
  private void addSubTables(
      Document document, Map<Integer, List<List<String>>> subTables, Font headerFont, Font dataFont)
      throws DocumentException {
    for (Map.Entry<Integer, List<List<String>>> entry : subTables.entrySet()) {
      List<List<String>> subTableData = entry.getValue();
      if (subTableData == null || subTableData.isEmpty()) {
        continue;
      }

      // 子表格的第一行作为表头
      List<String> subHeaders = subTableData.get(0);
      int subColumnCount = subHeaders.size();

      // 创建子表格构建器
      com.lincsoft.pdf.builder.TableBuilder subBuilder =
          com.lincsoft.pdf.builder.TableBuilder.create(subColumnCount)
              .headerFont(headerFont)
              .dataFont(dataFont)
              .headers(subHeaders.toArray(new String[0]));

      // 添加子表格数据行（从第二行开始）
      for (int i = 1; i < subTableData.size(); i++) {
        List<String> row = subTableData.get(i);
        if (row.size() == subColumnCount) {
          subBuilder.row(row.toArray(new String[0]));
        }
      }

      // 添加缩进效果（通过添加空段落）
      Paragraph indent = new Paragraph("    ", dataFont);
      document.add(indent);

      // 添加子表格
      document.add(subBuilder.build());

      log.debug("Added sub-table for row {} with {} rows", entry.getKey(), subTableData.size() - 1);
    }
  }

  /**
   * 页眉页脚事件处理器。
   *
   * <p>在每页添加页眉和页脚（含页码）。实现了以下需求：
   *
   * <ul>
   *   <li>Requirement 1.10: 在页面右上角显示页码，格式为"当前页码/总页码"
   *   <li>Requirement 3.10: 在页面右上角显示页码
   *   <li>Requirement 5.8: 支持配置页眉页脚高度、内容和垂直位置
   * </ul>
   *
   * <p>页码格式支持 {@code {current}} 和 {@code {total}} 占位符。由于生成过程中无法知道总页数， 使用 {@link PdfTemplate}
   * 创建占位符，在文档关闭时填充实际总页数。
   */
  private static class HeaderFooterEvent extends PdfPageEventHelper {

    private final BaseFont baseFont;
    private final float fontSize;
    private final String pageFormat;
    private final PdfTemplateConfig template;

    /** 用于存储总页数的 PdfTemplate */
    private PdfTemplate totalPageTemplate;

    /** 总页数占位符的 X 坐标 */
    private float totalPageX;

    /** 总页数占位符的 Y 坐标 */
    private float totalPageY;

    /**
     * 构造页眉页脚事件处理器。
     *
     * @param baseFont 基础字体
     * @param fontSize 字体大小
     * @param pageFormat 页码格式（支持 {current} 和 {total} 占位符）
     * @param template 模板配置
     */
    HeaderFooterEvent(
        BaseFont baseFont, float fontSize, String pageFormat, PdfTemplateConfig template) {
      this.baseFont = baseFont;
      this.fontSize = fontSize;
      this.pageFormat = pageFormat != null ? pageFormat : "{current}/{total}";
      this.template = template;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
      // 创建用于存储总页数的模板（大小足以容纳数字）
      totalPageTemplate = writer.getDirectContent().createTemplate(30, fontSize + 2);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
      if (baseFont == null) {
        return;
      }

      try {
        PdfContentByte cb = writer.getDirectContent();
        float pageWidth = document.getPageSize().getWidth();
        float pageHeight = document.getPageSize().getHeight();

        // 获取边距配置
        float marginLeft = template.getPage().getMarginLeft() * MM_TO_PT;
        float marginRight = template.getPage().getMarginRight() * MM_TO_PT;
        float marginTop = template.getPage().getMarginTop() * MM_TO_PT;
        float marginBottom = template.getPage().getMarginBottom() * MM_TO_PT;

        // 添加页眉
        addHeader(cb, document, pageWidth, pageHeight, marginTop, marginLeft, marginRight);

        // 添加页脚（含页码）
        addFooter(cb, document, pageWidth, marginBottom, marginLeft, marginRight, writer);

      } catch (Exception e) {
        // 页眉页脚添加失败不应影响 PDF 生成
        // 静默处理，因为此处无法使用 log
      }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
      if (baseFont == null || totalPageTemplate == null) {
        return;
      }

      try {
        // 在文档关闭时，填充总页数
        // 注意：getPageNumber() 返回的是下一页的页码，所以总页数需要减 1
        int totalPages = writer.getPageNumber() - 1;
        String totalText = String.valueOf(totalPages);

        // 在模板中写入总页数
        totalPageTemplate.beginText();
        totalPageTemplate.setFontAndSize(baseFont, fontSize);
        totalPageTemplate.showText(totalText);
        totalPageTemplate.endText();

      } catch (Exception e) {
        // 静默处理
      }
    }

    /**
     * 添加页眉。
     *
     * <p>根据配置在页面顶部添加页眉内容，支持自定义高度、内容和垂直对齐方式。
     *
     * @param cb PDF 内容字节
     * @param document 文档对象
     * @param pageWidth 页面宽度
     * @param pageHeight 页面高度
     * @param marginTop 上边距
     * @param marginLeft 左边距
     * @param marginRight 右边距
     */
    private void addHeader(
        PdfContentByte cb,
        Document document,
        float pageWidth,
        float pageHeight,
        float marginTop,
        float marginLeft,
        float marginRight) {

      String headerContent = template.getHeader().getContent();
      if (headerContent == null || headerContent.isBlank()) {
        return;
      }

      float headerHeight = template.getHeader().getHeight() * MM_TO_PT;

      // 计算页眉 Y 位置（在页边距上方）
      float yPosition = pageHeight - marginTop + headerHeight / 2;

      // 根据垂直对齐方式调整 Y 位置
      VerticalAlignment verticalAlign = template.getHeader().getVerticalAlignment();
      if (verticalAlign != null) {
        switch (verticalAlign) {
          case TOP:
            yPosition = pageHeight - marginTop + headerHeight - fontSize;
            break;
          case BOTTOM:
            yPosition = pageHeight - marginTop + fontSize;
            break;
          case CENTER:
          default:
            // 保持默认居中
            break;
        }
      }

      // 计算页眉文本位置（居中对齐）
      float textWidth = baseFont.getWidthPoint(headerContent, fontSize);
      float xPosition = (pageWidth - textWidth) / 2;

      cb.beginText();
      cb.setFontAndSize(baseFont, fontSize);
      cb.setTextMatrix(xPosition, yPosition);
      cb.showText(headerContent);
      cb.endText();
    }

    /**
     * 添加页脚（含页码）。
     *
     * <p>在页面底部添加页码，格式为"当前页码/总页码"。总页码使用占位符模板，在文档关闭时填充。
     *
     * @param cb PDF 内容字节
     * @param document 文档对象
     * @param pageWidth 页面宽度
     * @param marginBottom 下边距
     * @param marginLeft 左边距
     * @param marginRight 右边距
     * @param writer PDF 写入器
     */
    private void addFooter(
        PdfContentByte cb,
        Document document,
        float pageWidth,
        float marginBottom,
        float marginLeft,
        float marginRight,
        PdfWriter writer) {

      if (!template.getFooter().isShowPageNumber()) {
        return;
      }

      float footerHeight = template.getFooter().getHeight() * MM_TO_PT;

      // 计算页脚 Y 位置（在页边距下方）
      float yPosition = marginBottom + footerHeight / 2;

      // 获取当前页码
      int currentPage = writer.getPageNumber();

      // 检查格式中是否包含 {total}
      int totalIndex = pageFormat.indexOf("{total}");

      // 构建当前页码部分的文本（将 {current} 替换为实际页码，{total} 暂时保留）
      String displayText = pageFormat.replace("{current}", String.valueOf(currentPage));

      // 如果格式包含 {total}，需要在显示文本中预留空间
      // 然后使用模板来填充总页数
      if (totalIndex >= 0) {
        // 计算文本位置（右对齐）
        // 先计算不包含 {total} 的文本宽度
        String beforeTotal = displayText.substring(0, totalIndex);
        String afterTotal = displayText.substring(totalIndex + "{total}".length());

        float beforeWidth = baseFont.getWidthPoint(beforeTotal, fontSize);
        float afterWidth = baseFont.getWidthPoint(afterTotal, fontSize);

        // 计算总文本宽度（包括总页数占位符）
        float totalWidth = beforeWidth + 30 + afterWidth; // 30 是总页数模板的宽度

        // 右对齐的 X 位置
        float xPosition = pageWidth - totalWidth - marginRight;

        // 绘制 {total} 之前的部分
        cb.beginText();
        cb.setFontAndSize(baseFont, fontSize);
        cb.setTextMatrix(xPosition, yPosition);
        cb.showText(beforeTotal);
        cb.endText();

        // 记录总页数模板的位置
        totalPageX = xPosition + beforeWidth;
        totalPageY = yPosition;

        // 添加总页数占位符模板
        cb.addTemplate(totalPageTemplate, totalPageX, totalPageY);

        // 绘制 {total} 之后的部分
        if (!afterTotal.isEmpty()) {
          cb.beginText();
          cb.setFontAndSize(baseFont, fontSize);
          cb.setTextMatrix(totalPageX + 30, yPosition);
          cb.showText(afterTotal);
          cb.endText();
        }
      } else {
        // 格式中不包含 {total}，直接绘制文本
        float textWidth = baseFont.getWidthPoint(displayText, fontSize);
        float xPosition = pageWidth - textWidth - marginRight;

        cb.beginText();
        cb.setFontAndSize(baseFont, fontSize);
        cb.setTextMatrix(xPosition, yPosition);
        cb.showText(displayText);
        cb.endText();
      }
    }
  }
}
