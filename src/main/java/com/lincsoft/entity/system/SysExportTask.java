package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System Export Task Entity.
 *
 * <p>Records metadata of async export tasks, including file path, status, and expiration.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
@Data
@TableName("sys_export_task")
public class SysExportTask implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** UUID task identifier */
  private String taskId;

  /** Export type (e.g. LOG_TRACE) */
  private String type;

  /** Task status: PENDING/RUNNING/SUCCESS/FAILED/EXPIRED */
  private String status;

  /** Relative path from export directory */
  private String filePath;

  /** User-friendly download filename */
  private String fileName;

  /** File size in bytes */
  private Long fileSize;

  /** Number of trace records exported */
  private Integer rowCount;

  /** Task completion timestamp */
  private LocalDateTime completedAt;

  /** File expiration timestamp */
  private LocalDateTime expireAt;

  /** Error message (truncated to 1000 chars) */
  private String errorMessage;

  /** Creator username */
  private String createdBy;

  /** Record creation timestamp */
  private LocalDateTime createdAt;
}
