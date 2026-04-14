package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System Error Log Entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Data
@TableName("sys_error_log")
public class SysErrorLog implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** Trace ID */
  private String traceId;

  /** Exception File Name */
  private String exceptionFile;

  /** Exception Class Name */
  private String exceptionClass;

  /** Exception Method Name */
  private String exceptionMethod;

  /** Exception Line Number */
  private Integer exceptionLine;

  /** Exception Message */
  private String exceptionMessage;

  /** Root Cause Message */
  private String rootCauseMessage;

  /** Stack Trace */
  private String stackTrace;

  /** Request Method (GET/POST/PUT/DELETE) */
  private String requestMethod;

  /** Request URL */
  private String requestUrl;

  /** Client IP Address */
  private String clientIp;

  /** Operating Username */
  private String username;

  /** Record Creation Time */
  private LocalDateTime createTime;
}
