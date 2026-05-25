package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * Access log detail response.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param username Username
 * @param method HTTP method
 * @param path API path
 * @param queryString Query string
 * @param requestBody Request body
 * @param responseBody Response body
 * @param statusCode Business status code (from response body `code` field)
 * @param duration Processing duration (milliseconds)
 * @param clientIp Client IP
 * @param userAgent User-Agent
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record AccessLogDetailResponse(
    Long id,
    String traceId,
    String username,
    String method,
    String path,
    String queryString,
    String requestBody,
    String responseBody,
    int statusCode,
    Long duration,
    String clientIp,
    String userAgent,
    LocalDateTime createdAt) {}
