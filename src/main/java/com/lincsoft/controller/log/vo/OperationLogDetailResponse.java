package com.lincsoft.controller.log.vo;

import com.lincsoft.constant.OperationEnums;
import java.time.LocalDateTime;

/**
 * Operation log detail response.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param module Module
 * @param subModule Sub module
 * @param operationType Operation type
 * @param description Operation description
 * @param duration Processing duration (milliseconds)
 * @param requestMethod Request method
 * @param requestUrl Request URL
 * @param clientIp Client IP
 * @param username Username
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record OperationLogDetailResponse(
    Long id,
    String traceId,
    String module,
    String subModule,
    OperationEnums operationType,
    String description,
    Long duration,
    String requestMethod,
    String requestUrl,
    String clientIp,
    String username,
    LocalDateTime createdAt) {}
