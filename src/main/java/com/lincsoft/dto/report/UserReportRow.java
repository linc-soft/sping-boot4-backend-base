package com.lincsoft.dto.report;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Row DTO for user report joined queries.
 *
 * <p>Maps the result of a single SQL JOIN query that fetches user + role data together, avoiding
 * N+1 queries. Each row represents one user-role relationship. For users without roles, role fields
 * are null.
 *
 * @author 林创科技
 * @since 2026-05-25
 */
@Data
public class UserReportRow {
  private Long userId;
  private String username;
  private String status;
  private String createBy;
  private String updateBy;
  private LocalDateTime createAt;
  private LocalDateTime updateAt;

  private Long roleId;
  private String roleName;
  private String roleCode;
}
