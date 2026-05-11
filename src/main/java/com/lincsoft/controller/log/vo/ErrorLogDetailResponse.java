package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * Error log detail response.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param errorType Error type
 * @param message Error message
 * @param stackTrace Stack trace
 * @param username Username
 * @param requestMethod Request method
 * @param requestPath Request path
 * @param requestBody Request body
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record ErrorLogDetailResponse(
    Long id,
    String traceId,
    String errorType,
    String message,
    String stackTrace,
    String username,
    String requestMethod,
    String requestPath,
    String requestBody,
    LocalDateTime createdAt) {}
