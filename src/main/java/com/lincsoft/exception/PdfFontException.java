package com.lincsoft.exception;

import java.io.Serial;

/**
 * PDF 字体加载异常。
 *
 * <p>当字体文件加载失败时抛出此异常。
 *
 * <p>Validates: Requirements 8.2, 8.3, 8.5
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfFontException extends PdfException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码：字体错误 */
  public static final String ERROR_CODE = "PDF_FONT_ERROR";

  /**
   * 构造字体异常。
   *
   * @param message 错误消息
   */
  public PdfFontException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * 构造字体异常，包含原因异常。
   *
   * @param message 错误消息
   * @param cause 原因异常
   */
  public PdfFontException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
