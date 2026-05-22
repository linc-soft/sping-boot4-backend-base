package com.lincsoft.pdf.builder;

import static org.junit.jupiter.api.Assertions.*;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TableBuilder 单元测试。
 *
 * <p>测试表格构建器的基本功能，包括创建表格、设置表头、添加数据行、设置列宽等。
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@DisplayName("TableBuilder 单元测试")
class TableBuilderTest {

  @Nested
  @DisplayName("create() 方法测试")
  class CreateTest {

    @Test
    @DisplayName("创建表格构建器 - 正常情况")
    void testCreateNormal() {
      TableBuilder builder = TableBuilder.create(3);
      assertNotNull(builder);
      assertEquals(3, builder.getColumnCount());
      assertEquals(0, builder.getRowCount());
    }

    @Test
    @DisplayName("创建表格构建器 - 列数为 0 时抛出异常")
    void testCreateWithZeroColumns() {
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> TableBuilder.create(0));
      assertTrue(exception.getMessage().contains("列数必须大于 0"));
    }

    @Test
    @DisplayName("创建表格构建器 - 列数为负数时抛出异常")
    void testCreateWithNegativeColumns() {
      IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> TableBuilder.create(-1));
      assertTrue(exception.getMessage().contains("列数必须大于 0"));
    }
  }

  @Nested
  @DisplayName("headers() 方法测试")
  class HeadersTest {

    @Test
    @DisplayName("设置表头 - 正常情况")
    void testHeadersNormal() {
      TableBuilder builder = TableBuilder.create(3).headers("姓名", "年龄", "城市");
      assertNotNull(builder);
    }

    @Test
    @DisplayName("设置表头 - 数量不匹配时抛出异常")
    void testHeadersCountMismatch() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TableBuilder.create(3).headers("姓名", "年龄"));
      assertTrue(exception.getMessage().contains("表头数量必须与列数一致"));
    }

    @Test
    @DisplayName("设置表头 - null 时抛出异常")
    void testHeadersNull() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TableBuilder.create(3).headers((String[]) null));
      assertTrue(exception.getMessage().contains("表头数量必须与列数一致"));
    }

    @Test
    @DisplayName("设置表头 - 重复设置时抛出异常")
    void testHeadersAlreadySet() {
      IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              () -> TableBuilder.create(3).headers("姓名", "年龄", "城市").headers("A", "B", "C"));
      assertTrue(exception.getMessage().contains("表头已经设置过"));
    }
  }

  @Nested
  @DisplayName("row() 方法测试")
  class RowTest {

    @Test
    @DisplayName("添加数据行 - 正常情况")
    void testRowNormal() {
      TableBuilder builder = TableBuilder.create(3).headers("姓名", "年龄", "城市").row("张三", "25", "北京");

      assertEquals(1, builder.getRowCount());
    }

    @Test
    @DisplayName("添加多个数据行")
    void testMultipleRows() {
      TableBuilder builder =
          TableBuilder.create(3)
              .headers("姓名", "年龄", "城市")
              .row("张三", "25", "北京")
              .row("李四", "30", "上海")
              .row("王五", "28", "广州");

      assertEquals(3, builder.getRowCount());
    }

    @Test
    @DisplayName("添加数据行 - 数量不匹配时抛出异常")
    void testRowCountMismatch() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TableBuilder.create(3).headers("姓名", "年龄", "城市").row("张三", "25"));
      assertTrue(exception.getMessage().contains("单元格数量必须与列数一致"));
    }

    @Test
    @DisplayName("添加数据行 - null 时抛出异常")
    void testRowNull() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> TableBuilder.create(3).headers("姓名", "年龄", "城市").row((String[]) null));
      assertTrue(exception.getMessage().contains("单元格数量必须与列数一致"));
    }
  }

  @Nested
  @DisplayName("widths() 方法测试")
  class WidthsTest {

    @Test
    @DisplayName("设置列宽 - 正常情况")
    void testWidthsNormal() {
      TableBuilder builder = TableBuilder.create(3).widths(30f, 20f, 50f);
      assertNotNull(builder);
    }

    @Test
    @DisplayName("设置列宽 - 数量不匹配时抛出异常")
    void testWidthsCountMismatch() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TableBuilder.create(3).widths(30f, 20f));
      assertTrue(exception.getMessage().contains("宽度数量必须与列数一致"));
    }

    @Test
    @DisplayName("设置列宽 - null 时抛出异常")
    void testWidthsNull() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TableBuilder.create(3).widths((float[]) null));
      assertTrue(exception.getMessage().contains("宽度数量必须与列数一致"));
    }

    @Test
    @DisplayName("设置列宽 - 包含负值时抛出异常")
    void testWidthsWithNegativeValue() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TableBuilder.create(3).widths(30f, -1f, 50f));
      assertTrue(exception.getMessage().contains("列宽必须大于 0"));
    }

    @Test
    @DisplayName("设置列宽 - 包含 0 值时抛出异常")
    void testWidthsWithZeroValue() {
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> TableBuilder.create(3).widths(30f, 0f, 50f));
      assertTrue(exception.getMessage().contains("列宽必须大于 0"));
    }
  }

  @Nested
  @DisplayName("build() 方法测试")
  class BuildTest {

    @Test
    @DisplayName("构建表格 - 完整流程")
    void testBuildComplete() {
      PdfPTable table =
          TableBuilder.create(3)
              .headers("姓名", "年龄", "城市")
              .widths(30f, 20f, 50f)
              .row("张三", "25", "北京")
              .row("李四", "30", "上海")
              .build();

      assertNotNull(table);
      assertEquals(3, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("构建表格 - 无表头")
    void testBuildWithoutHeaders() {
      PdfPTable table = TableBuilder.create(3).widths(30f, 20f, 50f).row("张三", "25", "北京").build();

      assertNotNull(table);
      assertEquals(3, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("构建表格 - 无列宽（使用等宽布局）")
    void testBuildWithoutWidths() {
      PdfPTable table =
          TableBuilder.create(3).headers("姓名", "年龄", "城市").row("张三", "25", "北京").build();

      assertNotNull(table);
      assertEquals(3, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("构建表格 - 空表格（仅表头）")
    void testBuildEmptyTable() {
      PdfPTable table = TableBuilder.create(3).headers("姓名", "年龄", "城市").build();

      assertNotNull(table);
      assertEquals(3, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("构建表格 - 验证表头行设置")
    void testBuildHeaderRows() {
      PdfPTable table =
          TableBuilder.create(3).headers("姓名", "年龄", "城市").row("张三", "25", "北京").build();

      assertEquals(1, table.getHeaderRows());
    }
  }

  @Nested
  @DisplayName("链式调用测试")
  class ChainingTest {

    @Test
    @DisplayName("完整链式调用")
    void testFullChaining() {
      PdfPTable table =
          TableBuilder.create(4)
              .headers("角色名称", "角色CODE", "说明", "创建者")
              .widths(25f, 20f, 35f, 20f)
              .row("管理员", "ADMIN", "系统管理员", "system")
              .row("用户", "USER", "普通用户", "system")
              .build();

      assertNotNull(table);
      assertEquals(4, table.getNumberOfColumns());
    }
  }

  @Nested
  @DisplayName("自定义字体测试")
  class FontTest {

    @Test
    @DisplayName("设置表头字体")
    void testHeaderFont() {
      Font customFont = new Font(Font.HELVETICA, 12, Font.BOLD);
      PdfPTable table =
          TableBuilder.create(3)
              .headerFont(customFont)
              .headers("姓名", "年龄", "城市")
              .row("张三", "25", "北京")
              .build();

      assertNotNull(table);
    }

    @Test
    @DisplayName("设置数据行字体")
    void testDataFont() {
      Font customFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
      PdfPTable table =
          TableBuilder.create(3)
              .dataFont(customFont)
              .headers("姓名", "年龄", "城市")
              .row("张三", "25", "北京")
              .build();

      assertNotNull(table);
    }

    @Test
    @DisplayName("同时设置表头和数据行字体")
    void testBothFonts() {
      Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
      Font dataFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
      PdfPTable table =
          TableBuilder.create(3)
              .headerFont(headerFont)
              .dataFont(dataFont)
              .headers("姓名", "年龄", "城市")
              .row("张三", "25", "北京")
              .build();

      assertNotNull(table);
    }
  }

  @Nested
  @DisplayName("边界情况测试")
  class EdgeCaseTest {

    @Test
    @DisplayName("单列表格")
    void testSingleColumn() {
      PdfPTable table = TableBuilder.create(1).headers("姓名").row("张三").row("李四").build();

      assertNotNull(table);
      assertEquals(1, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("大列表格")
    void testLargeColumnCount() {
      String[] headers = new String[10];
      String[] row = new String[10];
      for (int i = 0; i < 10; i++) {
        headers[i] = "列" + (i + 1);
        row[i] = "值" + (i + 1);
      }

      PdfPTable table = TableBuilder.create(10).headers(headers).row(row).build();

      assertNotNull(table);
      assertEquals(10, table.getNumberOfColumns());
    }

    @Test
    @DisplayName("单元格值为 null")
    void testNullCellValue() {
      PdfPTable table =
          TableBuilder.create(3).headers("姓名", "年龄", "城市").row(null, "25", "北京").build();

      assertNotNull(table);
    }

    @Test
    @DisplayName("单元格值为空字符串")
    void testEmptyCellValue() {
      PdfPTable table =
          TableBuilder.create(3).headers("姓名", "年龄", "城市").row("", "25", "北京").build();

      assertNotNull(table);
    }
  }
}
