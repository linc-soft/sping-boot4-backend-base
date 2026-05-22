package com.lincsoft.pdf.dto;

import com.lincsoft.pdf.config.PdfTemplateConfig;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF 配置
 *
 * <p>定义 PDF 文档的标题、查询条件、模板配置和文件名模板。
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
public class PdfConfig {

  /** 文档标题 */
  private String title;

  /** 查询条件描述 */
  private List<String> queryConditions;

  /** 模板配置 */
  private PdfTemplateConfig template;

  /** 文件名模板 */
  private String filenamePattern;
}
