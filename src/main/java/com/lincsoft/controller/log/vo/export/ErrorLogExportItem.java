package com.lincsoft.controller.log.vo.export;

import java.time.LocalDateTime;

/**
 * Error log export item — only the 8 specified fields.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
public record ErrorLogExportItem(
    String exceptionFile,
    String exceptionClass,
    String exceptionMethod,
    Integer exceptionLine,
    String exceptionMessage,
    String rootCauseMessage,
    String stackTrace,
    LocalDateTime createTime) {}
