package com.lincsoft.exception;

import java.io.Serial;

/**
 * PDF 语言环境异常。
 *
 * <p>当语言资源加载或处理失败时抛出此异常。系统应回退到默认语言（英文）继续处理。
 *
 * <p>Validates: Requirements 6.2, 6.4, 6.7
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfLocaleException extends PdfException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码：语言错误 */
  public static final String ERROR_CODE = "PDF_LOCALE_ERROR";

  /**
   * 构造语言环境异常。
   *
   * @param message 错误消息
   */
  public PdfLocaleException(String message) {
    super(ERROR_CODE, message);
  }

  /**
   * 构造语言环境异常，包含原因异常。
   *
   * @param message 错误消息
   * @param cause 原因异常
   */
  public PdfLocaleException(String message, Throwable cause) {
    super(ERROR_CODE, message, cause);
  }
}
