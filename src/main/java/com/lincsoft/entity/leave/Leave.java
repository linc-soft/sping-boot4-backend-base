package com.lincsoft.entity.leave;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Leave request entity.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Data
@TableName("txn_leave")
public class Leave {

  private Long id;

  /** FK → mst_employee.user_id → mst_user.id */
  @TableField("user_id")
  private Long userId;

  /** Leave start date */
  private LocalDate startDate;

  /** Leave end date */
  private LocalDate endDate;

  /** Leave type (0=Annual,1=Personal,2=Sick,3=Marriage,4=Paternity,5=Bereavement) */
  @TableField("leave_type")
  private Integer leaveType;

  /** Leave duration in days (multiple of 0.5) */
  private BigDecimal duration;

  /** Leave reason */
  private String reason;

  /** Status (0=Applying,1=Approved,2=Rejected) */
  private Integer status;

  /** Approver FK → mst_user.id */
  @TableField("approver_id")
  private Long approverId;

  /** Approval time */
  @TableField("approve_time")
  private LocalDateTime approveTime;

  /** Approval/rejection reason */
  @TableField("approve_reason")
  private String approveReason;

  private String createBy;
  private LocalDateTime createAt;
  private String updateBy;
  private LocalDateTime updateAt;
  private Integer deleted;
}
