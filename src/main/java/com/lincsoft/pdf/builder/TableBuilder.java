package com.lincsoft.pdf.builder;

import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * PDF 表格构建器。
 *
 * <p>提供流畅的 Builder API 用于构建 PDF 表格。支持设置表头、添加数据行、设置列宽等功能。
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * PdfPTable table = TableBuilder.create(3)
 *     .headers("姓名", "年龄", "城市")
 *     .widths(30f, 20f, 50f)
 *     .row("张三", "25", "北京")
 *     .row("李四", "30", "上海")
 *     .build();
 * }</pre>
 *
 * <p><b>Validates: Requirements 1.11, 1.12, 2.6, 2.7, 3.11, 4.6, 4.7</b>
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Slf4j
public class TableBuilder {

  /** 列数 */
  private final int columnCount;

  /** 表头列表 */
  private String[] headers;

  /** 数据行列表 */
  private final List<String[]> rows = new ArrayList<>();

  /** 列宽数组 */
  private float[] widths;

  /** 表头字体 */
  private Font headerFont;

  /** 数据行字体 */
  private Font dataFont;

  /** 是否已调用 headers() 方法 */
  private boolean headersSet = false;

  /**
   * 私有构造函数。
   *
   * @param columnCount 列数
   */
  private TableBuilder(int columnCount) {
    if (columnCount <= 0) {
      throw new IllegalArgumentException("列数必须大于 0，当前值: " + columnCount);
    }
    this.columnCount = columnCount;
  }

  /**
   * 创建表格构建器。
   *
   * <p>创建一个指定列数的表格构建器实例。
   *
   * @param columnCount 列数，必须大于 0
   * @return TableBuilder 实例
   * @throws IllegalArgumentException 如果列数小于或等于 0
   */
  public static TableBuilder create(int columnCount) {
    return new TableBuilder(columnCount);
  }

  /**
   * 设置表头。
   *
   * <p>设置表格的表头行。表头列数必须与创建时指定的列数一致。
   *
   * @param headers 表头列表
   * @return this，支持链式调用
   * @throws IllegalArgumentException 如果表头数量与列数不匹配
   * @throws IllegalStateException 如果表头已经被设置过
   */
  public TableBuilder headers(String... headers) {
    if (headersSet) {
      throw new IllegalStateException("表头已经设置过，不能重复设置");
    }
    if (headers == null || headers.length != columnCount) {
      throw new IllegalArgumentException(
          "表头数量必须与列数一致，期望: " + columnCount + "，实际: " + (headers == null ? 0 : headers.length));
    }
    this.headers = headers.clone();
    this.headersSet = true;
    return this;
  }

  /**
   * 添加数据行。
   *
   * <p>向表格添加一行数据。单元格数量必须与列数一致。
   *
   * @param cells 单元格值
   * @return this，支持链式调用
   * @throws IllegalArgumentException 如果单元格数量与列数不匹配
   */
  public TableBuilder row(String... cells) {
    if (cells == null || cells.length != columnCount) {
      throw new IllegalArgumentException(
          "单元格数量必须与列数一致，期望: " + columnCount + "，实际: " + (cells == null ? 0 : cells.length));
    }
    rows.add(cells.clone());
    return this;
  }

  /**
   * 设置列宽。
   *
   * <p>设置各列的宽度比例。宽度数量必须与列数一致。
   *
   * @param widths 宽度数组
   * @return this，支持链式调用
   * @throws IllegalArgumentException 如果宽度数量与列数不匹配，或包含负值
   */
  public TableBuilder widths(float... widths) {
    if (widths == null || widths.length != columnCount) {
      throw new IllegalArgumentException(
          "宽度数量必须与列数一致，期望: " + columnCount + "，实际: " + (widths == null ? 0 : widths.length));
    }
    for (int i = 0; i < widths.length; i++) {
      if (widths[i] <= 0) {
        throw new IllegalArgumentException("列宽必须大于 0，第 " + (i + 1) + " 列宽度: " + widths[i]);
      }
    }
    this.widths = widths.clone();
    return this;
  }

  /**
   * 设置表头字体。
   *
   * @param font 字体
   * @return this，支持链式调用
   */
  public TableBuilder headerFont(Font font) {
    this.headerFont = font;
    return this;
  }

  /**
   * 设置数据行字体。
   *
   * @param font 字体
   * @return this，支持链式调用
   */
  public TableBuilder dataFont(Font font) {
    this.dataFont = font;
    return this;
  }

  /**
   * 构建 PdfPTable。
   *
   * <p>根据设置的表头、数据行和列宽构建 OpenPDF 的 PdfPTable 对象。
   *
   * <p>如果没有设置列宽，将使用等宽布局。 如果没有设置表头字体，将使用默认字体（大小 10，粗体）。 如果没有设置数据行字体，将使用默认字体（大小 10）。
   *
   * @return PdfPTable 对象
   */
  public PdfPTable build() {
    try {
      PdfPTable table = new PdfPTable(columnCount);

      // 设置列宽
      if (widths != null) {
        table.setWidths(widths);
      }

      // 设置表头
      if (headers != null) {
        for (String header : headers) {
          PdfPCell cell = createHeaderCell(header);
          table.addCell(cell);
        }
        table.setHeaderRows(1);
      }

      // 添加数据行
      for (String[] row : rows) {
        for (String cellValue : row) {
          PdfPCell cell = createDataCell(cellValue);
          table.addCell(cell);
        }
      }

      return table;
    } catch (Exception e) {
      log.error("构建表格失败: {}", e.getMessage(), e);
      throw new RuntimeException("构建表格失败: " + e.getMessage(), e);
    }
  }

  /**
   * 创建表头单元格。
   *
   * @param value 单元格值
   * @return PdfPCell 对象
   */
  private PdfPCell createHeaderCell(String value) {
    Font font = headerFont != null ? headerFont : createDefaultHeaderFont();
    Paragraph paragraph = new Paragraph(value != null ? value : "", font);
    PdfPCell cell = new PdfPCell(paragraph);
    cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
    cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
    return cell;
  }

  /**
   * 创建数据单元格。
   *
   * @param value 单元格值
   * @return PdfPCell 对象
   */
  private PdfPCell createDataCell(String value) {
    Font font = dataFont != null ? dataFont : createDefaultDataFont();
    Paragraph paragraph = new Paragraph(value != null ? value : "", font);
    PdfPCell cell = new PdfPCell(paragraph);
    cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
    cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
    return cell;
  }

  /**
   * 创建默认表头字体。
   *
   * @return Font 对象
   */
  private Font createDefaultHeaderFont() {
    return new Font(Font.HELVETICA, 10, Font.BOLD);
  }

  /**
   * 创建默认数据字体。
   *
   * @return Font 对象
   */
  private Font createDefaultDataFont() {
    return new Font(Font.HELVETICA, 10, Font.NORMAL);
  }

  /**
   * 获取列数。
   *
   * @return 列数
   */
  public int getColumnCount() {
    return columnCount;
  }

  /**
   * 获取数据行数。
   *
   * @return 数据行数
   */
  public int getRowCount() {
    return rows.size();
  }
}
