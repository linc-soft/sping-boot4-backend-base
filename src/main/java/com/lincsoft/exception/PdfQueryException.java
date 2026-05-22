package com.lincsoft.exception;

import java.io.Serial;

/**
 * PDF 数据查询异常。
 *
 * <p>当数据查询过程中发生错误时抛出此异常。
 *
 * <p>Validates: Requirements 1.2, 2.2, 3.2, 4.2
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfQueryException extends PdfException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码：查询错误 */
  public static final String ERROR_CODE = "PDF_QUERY_ERROR";

  /**
   * 构造查询异常。
   *
   * @param message 错误消息
   */
  public PdfQueryException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * 构造查询异常，包含原因异常。
   *
   * @param message 错误消息
   * @param cause 原因异常
   */
  public PdfQueryException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
