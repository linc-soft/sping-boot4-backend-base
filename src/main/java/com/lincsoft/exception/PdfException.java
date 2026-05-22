package com.lincsoft.exception;

import java.io.Serial;
import lombok.Getter;

/**
 * PDF 生成基础异常类。
 *
 * <p>所有 PDF 生成相关的异常都应继承此类，提供统一的错误码管理。
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码，用于标识具体的错误类型 */
  @Getter private final String errorCode;

  /**
   * 构造 PDF 异常。
   *
   * @param errorCode 错误码
   * @param message 错误消息
   */
  public PdfException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * 构造 PDF 异常，包含原因异常。
   *
   * @param errorCode 错误码
   * @param message 错误消息
   * @param cause 原因异常
   */
  public PdfException(String errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
