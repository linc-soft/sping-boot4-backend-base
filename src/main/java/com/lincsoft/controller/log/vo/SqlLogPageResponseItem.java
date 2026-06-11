package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * SQL log page response item.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param sqlType SQL type (SELECT/INSERT/UPDATE/DELETE)
 * @param mapperMethod Mapper method
 * @param duration Processing duration (milliseconds)
 * @param username Username
 * @param isSlow Whether this is a slow query
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-06-11
 */
public record SqlLogPageResponseItem(
    Long id,
    String traceId,
    String sqlType,
    String mapperMethod,
    Long duration,
    String username,
    Boolean isSlow,
    LocalDateTime createdAt) {}
