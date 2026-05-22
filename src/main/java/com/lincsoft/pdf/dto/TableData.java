package com.lincsoft.pdf.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表格数据
 *
 * <p>定义 PDF 表格的数据结构，包含表头、数据行、列宽比例和子表格映射。
 *
 * <p>子表格映射用于在特定数据行下方显示额外的嵌套表格，例如显示角色的继承角色信息。
 *
 * <p><b>Validates: Requirements 1.8, 1.9, 2.5, 2.8, 3.8, 3.9, 4.5, 4.9</b>
 *
 * @author 林创科技
 * @since 2026-04-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableData {

  /** 表头 */
  private List<String> headers;

  /** 数据行 */
  private List<List<String>> rows;

  /** 列宽比例 */
  private float[] columnWidths;

  /** 子表格映射 (key: 数据行索引, value: 子表格数据) */
  private Map<Integer, List<List<String>>> subTables;
}
