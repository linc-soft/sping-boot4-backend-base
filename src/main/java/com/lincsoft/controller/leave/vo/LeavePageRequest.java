package com.lincsoft.controller.leave.vo;

import com.lincsoft.common.PageRequest;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Leave page query request VO.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LeavePageRequest extends PageRequest {

  /** User ID filter */
  private Long userId;

  /** Year filter */
  private Integer year;

  /** Leave type filter */
  private Integer leaveType;

  /** Leave status filter */
  private Integer status;

  public Set<String> allowedSortColumns() {
    return Set.of("start_date", "end_date", "duration", "status", "create_at");
  }
}
