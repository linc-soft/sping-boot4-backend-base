package com.lincsoft.exception;

import java.io.Serial;
import java.util.Map;

/**
 * PDF 验证异常。
 *
 * <p>当请求参数验证失败时抛出此异常。
 *
 * <p>Validates: Requirements 1.5, 1.7, 2.4, 3.6, 3.7, 4.4, 5.9
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class PdfValidationException extends PdfException {
  @Serial private static final long serialVersionUID = 1L;

  /** 错误码：验证错误 */
  public static final String ERROR_CODE = "PDF_VALIDATION_ERROR";

  /** 详细错误信息 */
  private final Map<String, String> details;

  /**
   * 构造验证异常。
   *
   * @param message 错误消息
   */
  public PdfValidationException(String message) {
    super(ERROR_CODE, message);
    this.details = null;
  }

  /**
   * 构造验证异常，包含详细错误信息。
   *
   * @param message 错误消息
   * @param details 详细错误信息
   */
  public PdfValidationException(String message, Map<String, String> details) {
    super(ERROR_CODE, message);
    this.details = details;
  }

  /**
   * 获取详细错误信息。
   *
   * @return 详细错误信息
   */
  public Map<String, String> getDetails() {
    return details;
  }
}
