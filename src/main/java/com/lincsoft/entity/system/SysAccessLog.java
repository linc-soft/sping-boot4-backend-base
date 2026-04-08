package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * System Access Log Entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Data
@TableName("sys_access_log")
public class SysAccessLog implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** Trace ID */
  private String traceId;

  /** Request method (GET/POST/PUT/DELETE) */
  private String requestMethod;

  /** Request URL */
  private String requestUrl;

  /** Request Parameters (JSON format) */
  private String requestParams;

  /** Request Headers (JSON format) */
  private String requestHeaders;

  /** Request Body (JSON format) */
  private String requestBody;

  /** Response Status Code */
  private Integer responseStatus;

  /** Response Headers (JSON format) */
  private String responseHeaders;

  /** Response Body (JSON format) */
  private String responseBody;

  /** Client IP Address */
  private String clientIp;

  /** User-Agent */
  private String userAgent;

  /** Operation Username */
  private String username;

  /** Request Processing Time (Milliseconds) */
  private Long duration;

  /** Record Creation Time */
  private LocalDateTime createTime;
}
