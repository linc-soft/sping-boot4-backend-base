package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * SQL log detail response.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param sqlText SQL text
 * @param duration Processing duration (milliseconds)
 * @param createTime Create time
 * @param mapperClass Mapper class
 * @param mapperMethod Mapper method
 * @param sqlType SQL type (SELECT/INSERT/UPDATE/DELETE)
 * @param username Username
 * @param requestUrl Request URL
 * @param requestMethod Request method
 * @param clientIp Client IP
 * @param sqlParams SQL parameters
 * @param rowCount Affected row count
 * @author 林创科技
 * @since 2026-06-11
 */
public record SqlLogDetailResponse(
    Long id,
    String traceId,
    String sqlText,
    Long duration,
    LocalDateTime createTime,
    String mapperClass,
    String mapperMethod,
    String sqlType,
    String username,
    String requestUrl,
    String requestMethod,
    String clientIp,
    String sqlParams,
    Long rowCount) {}
