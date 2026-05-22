package com.lincsoft.exception;

import java.io.Serial;

/**
 * PDF 无数据异常。
 *
 * <p>当查询结果为空，无法生成 PDF 时抛出此异常。
 *
 * <p>Validates: Requirements 1.3, 2.3
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfNoDataException extends PdfException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码：无数据 */
  public static final String ERROR_CODE = "PDF_NO_DATA";

  /**
   * 构造无数据异常。
   *
   * @param message 错误消息
   */
  public PdfNoDataException(String message) {
    super(ERROR_CODE, message);
  }
}
