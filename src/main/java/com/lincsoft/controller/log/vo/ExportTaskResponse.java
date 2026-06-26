package com.lincsoft.controller.log.vo;

import java.time.LocalDateTime;

/**
 * Export task response VO.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
public record ExportTaskResponse(
    String taskId,
    String type,
    String status,
    String fileName,
    Long fileSize,
    Integer rowCount,
    LocalDateTime completedAt,
    LocalDateTime expireAt,
    String errorMessage,
    String createdBy,
    LocalDateTime createdAt) {}
