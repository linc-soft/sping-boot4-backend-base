package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Leave request entity.
 *
 * <p>Stores the business data of a leave request. The approval flow itself is owned by the Flowable
 * BPMN engine; this entity links to the process instance via {@code processInstanceId}.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("oa_leave_request")
public class OaLeaveRequest extends VersionedEntity {
  /** Applicant employee ID (mst_employee.id) */
  private Long employeeId;

  /** Leave type (1 annual / 2 sick / 3 personal / 4 marriage / 5 maternity / 9 other) */
  private String leaveType;

  /** Leave start date */
  private LocalDate startDate;

  /** Leave start period ('0' = AM, '1' = PM) */
  private String startPeriod;

  /** Leave end date */
  private LocalDate endDate;

  /** Leave end period ('0' = AM, '1' = PM) */
  private String endPeriod;

  /** Number of leave days */
  private BigDecimal days;

  /** Leave reason */
  private String reason;

  /** Status (0 pending / 1 approved / 2 rejected / 3 withdrawn) */
  private String status;

  /** Flowable process instance ID */
  private String processInstanceId;

  /** Resolved approver employee ID (direct manager) */
  private Long approverId;

  /** Approver comment */
  private String approvalComment;
}
