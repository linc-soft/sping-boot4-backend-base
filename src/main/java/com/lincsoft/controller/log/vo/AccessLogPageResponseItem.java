package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * Access log page response item.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param username Username
 * @param method HTTP method
 * @param path API path
 * @param statusCode Business status code (from response body `code` field)
 * @param duration Processing duration (milliseconds)
 * @param clientIp Client IP
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record AccessLogPageResponseItem(
    Long id,
    String traceId,
    String username,
    String method,
    String path,
    int statusCode,
    Long duration,
    String clientIp,
    LocalDateTime createdAt) {}
