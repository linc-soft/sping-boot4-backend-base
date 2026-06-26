package com.lincsoft.controller.log.vo.export;

import java.time.LocalDateTime;

/**
 * Operation log export item — only the 6 specified fields.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
public record OperationLogExportItem(
    String module,
    String subModule,
    String operationType,
    String description,
    Long duration,
    LocalDateTime createTime) {}
