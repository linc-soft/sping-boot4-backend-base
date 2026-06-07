package com.lincsoft.controller.oa.vo;

import com.lincsoft.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee page query request VO.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeePageRequest extends PageRequest {
  /** Real name (partial match) */
  private String realName;

  /** Employee number (partial match) */
  private String employeeNo;

  /** Department ID (exact match) */
  private Long deptId;

  /** Employment status (exact match) */
  private String status;
}
