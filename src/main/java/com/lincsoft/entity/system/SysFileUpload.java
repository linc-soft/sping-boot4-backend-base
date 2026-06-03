package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System File Upload Entity.
 *
 * <p>Records metadata of uploaded files, including storage details and optional association
 * information for linking files to business entities.
 *
 * @author 林创科技
 * @since 2026-06-03
 */
@Data
@TableName("sys_file_upload")
public class SysFileUpload implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** File type (e.g., 1=image, 2=document, 3=archive) */
  private Integer fileType;

  /** UUID-based storage filename */
  private String storedName;

  /** Original filename from the client */
  private String originalFilename;

  /** File extension (lowercase, without dot) */
  private String extension;

  /** File size in bytes */
  private Long fileSize;

  /** MIME type of the file */
  private String contentType;

  /** Full file path relative to upload directory */
  private String filePath;

  /** Date-based subdirectory (e.g., "2026/06/03") */
  private String datePath;

  /** Date in URL-friendly format (e.g., "2026-06-03") */
  private String dateUrl;

  /** Association type */
  private String associateType;

  /** Associated business entity ID */
  private Long associateId;

  /** MD5 hash of the file content */
  private String md5;

  /** Create user */
  private String creator;

  /** Create time */
  private LocalDateTime createTime;

  /** Delete flag */
  @TableLogic private Integer deleted;
}
