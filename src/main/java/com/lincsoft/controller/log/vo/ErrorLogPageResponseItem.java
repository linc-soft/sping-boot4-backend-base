package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * Error log page response item.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param errorType Error type
 * @param message Error message
 * @param username Username
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record ErrorLogPageResponseItem(
    Long id,
    String traceId,
    String errorType,
    String message,
    String username,
    LocalDateTime createdAt) {}
