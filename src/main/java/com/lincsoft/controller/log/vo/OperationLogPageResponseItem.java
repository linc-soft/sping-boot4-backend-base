package com.lincsoft.controller.log.vo;

import com.lincsoft.constant.OperationEnums;
import java.time.LocalDateTime;

/**
 * Operation log page response item.
 *
 * @param id ID
 * @param traceId Trace ID
 * @param module Module
 * @param subModule Sub module
 * @param operationType Operation type
 * @param description Operation description
 * @param duration Processing duration (milliseconds)
 * @param username Username
 * @param createdAt Created at
 * @author 林创科技
 * @since 2026-05-11
 */
public record OperationLogPageResponseItem(
    Long id,
    String traceId,
    String module,
    String subModule,
    OperationEnums operationType,
    String description,
    Long duration,
    String username,
    LocalDateTime createdAt) {}
