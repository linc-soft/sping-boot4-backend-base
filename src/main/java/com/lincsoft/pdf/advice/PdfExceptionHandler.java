package com.lincsoft.pdf.advice;

import com.lincsoft.exception.PdfException;
import com.lincsoft.exception.PdfFontException;
import com.lincsoft.exception.PdfLocaleException;
import com.lincsoft.exception.PdfNoDataException;
import com.lincsoft.exception.PdfQueryException;
import com.lincsoft.exception.PdfValidationException;
import com.lincsoft.pdf.dto.PdfErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * PDF 异常处理器。
 *
 * <p>处理 PDF 生成过程中抛出的各种异常，返回对应的 HTTP 状态码和错误响应。
 *
 * <p>异常处理策略：
 *
 * <ul>
 *   <li>PdfNoDataException: HTTP 200，返回提示信息
 *   <li>PdfValidationException: HTTP 400，返回校验错误详情
 *   <li>PdfQueryException: HTTP 500，记录日志，返回通用错误
 *   <li>PdfFontException: HTTP 500，记录日志，返回字体加载错误
 *   <li>PdfLocaleException: HTTP 200，回退到默认语言处理
 * </ul>
 *
 * <p>Validates: Requirements 7.5, 7.6, 7.7
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Slf4j
@RestControllerAdvice
public class PdfExceptionHandler {

  /**
   * 处理无数据异常。
   *
   * <p>当查询结果为空时，返回 HTTP 200 和提示信息，允许客户端正常处理。
   *
   * @param e 无数据异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfNoDataException.class)
  public ResponseEntity<PdfErrorResponse> handleNoData(PdfNoDataException e) {
    log.info("PDF generation blocked: no data found - {}", e.getMessage());

    PdfErrorResponse response = PdfErrorResponse.of(e.getErrorCode(), e.getMessage());
    return ResponseEntity.ok(response);
  }

  /**
   * 处理验证异常。
   *
   * <p>当请求参数验证失败时，返回 HTTP 400 和详细错误信息。
   *
   * @param e 验证异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfValidationException.class)
  public ResponseEntity<PdfErrorResponse> handleValidation(PdfValidationException e) {
    log.warn("PDF validation error: {}", e.getMessage());

    PdfErrorResponse response =
        PdfErrorResponse.of(e.getErrorCode(), e.getMessage(), e.getDetails());
    return ResponseEntity.badRequest().body(response);
  }

  /**
   * 处理数据查询异常。
   *
   * <p>当数据库查询失败时，记录错误日志并返回 HTTP 500 通用错误信息。
   *
   * @param e 查询异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfQueryException.class)
  public ResponseEntity<PdfErrorResponse> handleQueryError(PdfQueryException e) {
    log.error("PDF query error occurred", e);

    PdfErrorResponse response =
        PdfErrorResponse.of(e.getErrorCode(), "Internal server error during data query");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * 处理字体加载异常。
   *
   * <p>当字体文件加载失败时，记录错误日志并返回 HTTP 500 错误信息。
   *
   * @param e 字体异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfFontException.class)
  public ResponseEntity<PdfErrorResponse> handleFontError(PdfFontException e) {
    log.error("Font loading error occurred", e);

    PdfErrorResponse response =
        PdfErrorResponse.of(e.getErrorCode(), "Font loading failed, please contact administrator");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  /**
   * 处理语言环境异常。
   *
   * <p>当语言处理出现问题时，返回 HTTP 200，系统将回退到默认语言（英文）。
   *
   * @param e 语言环境异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfLocaleException.class)
  public ResponseEntity<PdfErrorResponse> handleLocaleError(PdfLocaleException e) {
    log.warn("PDF locale error: {}, falling back to default language", e.getMessage());

    PdfErrorResponse response =
        PdfErrorResponse.of(e.getErrorCode(), "Locale processing error, using default language");
    return ResponseEntity.ok(response);
  }

  /**
   * 处理其他 PDF 异常。
   *
   * <p>作为兜底处理，捕获所有未明确处理的 PdfException 子类。
   *
   * @param e PDF 异常
   * @return 错误响应
   */
  @ExceptionHandler(PdfException.class)
  public ResponseEntity<PdfErrorResponse> handlePdfException(PdfException e) {
    log.error("Unexpected PDF error occurred: code={}", e.getErrorCode(), e);

    PdfErrorResponse response =
        PdfErrorResponse.of(e.getErrorCode(), "An error occurred during PDF generation");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
