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
    String requestHeaders,
    String requestBody,
    Integer responseStatus,
    String responseHeaders,
    String responseBody,
    String clientIp,
    String userAgent,
    String username,
    Long duration,
    LocalDateTime createTime) {}
