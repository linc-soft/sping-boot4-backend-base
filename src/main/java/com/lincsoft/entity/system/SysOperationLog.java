package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System Operation Log Entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** Trace ID */
  private String traceId;

  /** Primary Module */
  private String module;

  /** Secondary Module */
  private String subModule;

  /** Operation Type (CREATE/UPDATE/DELETE/QUERY) */
  private String operationType;

  /** Operation Description */
  private String description;

  /** Operation Processing Time (Milliseconds) */
  private Long duration;

  /** Record Creation Time */
  private LocalDateTime createTime;
}
