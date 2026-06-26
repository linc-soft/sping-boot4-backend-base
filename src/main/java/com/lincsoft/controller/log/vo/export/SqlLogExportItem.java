package com.lincsoft.controller.log.vo.export;

import java.time.LocalDateTime;

/**
 * SQL log export item — only the 8 specified fields.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
public record SqlLogExportItem(
    String sqlType,
    String sqlText,
    String sqlParams,
    String mapperClass,
    String mapperMethod,
    Long rowCount,
    Long duration,
    LocalDateTime createTime) {}
