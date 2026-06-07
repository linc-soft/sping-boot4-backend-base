package com.lincsoft.controller.oa.vo;

import com.lincsoft.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Leave request page query request VO.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LeavePageRequest extends PageRequest {
  /** Applicant employee ID (exact match) */
  private Long employeeId;

  /** Leave type (exact match) */
  private String leaveType;

  /** Status (exact match) */
  private String status;
}
