package com.lincsoft.controller.master.vo;

import lombok.Data;

/**
 * User list PDF report request.
 *
 * @author 林创科技
 * @since 2026-05-24
 */
@Data
public class UserListReportRequest {
  /** Username filter (partial match). */
  private String username;

  /**
   * Group field for report grouping.
   *
   * <p>Supported values:
   *
   * <ul>
   *   <li>{@code null} or blank - no grouping
   *   <li>{@code role} - group by directly assigned role
   *   <li>{@code baseRole} - group by base role (ancestor roles in the inheritance chain)
   * </ul>
   */
  private String groupBy;
}
