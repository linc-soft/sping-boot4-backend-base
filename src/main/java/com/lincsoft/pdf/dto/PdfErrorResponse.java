package com.lincsoft.pdf.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * PDF 错误响应 DTO。
 *
 * <p>用于封装 PDF 生成过程中的错误信息，返回给客户端。
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PdfErrorResponse implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码 */
  private String code;

  /** 错误消息 */
  private String message;

  /** 详细错误信息（可选） */
  private Map<String, String> details;

  /**
   * 创建简单的错误响应（无详细信息）。
   *
   * @param code 错误码
   * @param message 错误消息
   * @return 错误响应
   */
  public static PdfErrorResponse of(String code, String message) {
    return new PdfErrorResponse(code, message, null);
  }

  /**
   * 创建带详细信息的错误响应。
   *
   * @param code 错误码
   * @param message 错误消息
   * @param details 详细错误信息
   * @return 错误响应
   */
  public static PdfErrorResponse of(String code, String message, Map<String, String> details) {
    return new PdfErrorResponse(code, message, details);
  }
}
