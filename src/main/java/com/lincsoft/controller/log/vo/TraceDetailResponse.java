package com.lincsoft.controller.log.vo;

import java.util.List;

/**
 * Trace detail response.
 *
 * @param accessLog Access log detail
 * @param errorLog Error log detail (optional)
 * @param operationLogs Operation log list
 * @param sqlLogs SQL log list
 * @author 林创科技
 * @since 2026-05-11
 */
public record TraceDetailResponse(
    AccessLogDetailResponse accessLog,
    ErrorLogDetailResponse errorLog,
    List<OperationLogDetailResponse> operationLogs,
    List<SqlLogDetailResponse> sqlLogs) {}
