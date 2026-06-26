package com.lincsoft.controller.log.vo.export;

import java.time.LocalDateTime;

/**
 * Access log export item — only the fields specified for export (excludes id, traceId).
 *
 * @author 林创科技
 * @since 2026-06-26
 */
public record AccessLogExportItem(
    String requestMethod,
    String requestUrl,
    String requestParams,
    Object requestHeaders,
    Object requestBody,
    Integer responseStatus,
    Object responseHeaders,
    Object responseBody,
    String clientIp,
    String userAgent,
    String username,
    Long duration,
    LocalDateTime createTime) {}
