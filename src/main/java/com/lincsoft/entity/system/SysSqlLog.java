package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System SQL Log Entity
 *
 * @author 林创科技
 * @since 2026-06-11
 */
@Data
@TableName("sys_sql_log")
public class SysSqlLog implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** Trace ID */
  private String traceId;

  /** SQL Text */
  private String sqlText;

  /** Execution Duration (Milliseconds) */
  private Long duration;

  /** Record Creation Time */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** Mapper Class */
  private String mapperClass;

  /** Mapper Method */
  private String mapperMethod;

  /** SQL Type (SELECT/INSERT/UPDATE/DELETE) */
  @TableField("sql_type")
  private String sqlType;

  /** Operation Username */
  private String username;

  /** Request URL */
  private String requestUrl;

  /** Request Method (GET/POST/PUT/DELETE) */
  private String requestMethod;

  /** Client IP Address */
  private String clientIp;

  /** SQL Parameters (JSON format) */
  private String sqlParams;

  /** Affected Row Count */
  private Long rowCount;
}
