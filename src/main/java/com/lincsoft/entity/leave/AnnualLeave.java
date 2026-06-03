package com.lincsoft.entity.leave;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * Annual leave balance entity per user per year.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Data
@TableName("txn_annual_leave")
public class AnnualLeave {

  private Long id;

  /** FK → mst_employee.user_id → mst_user.id */
  @TableField("user_id")
  private Long userId;

  /** Calendar year */
  private Integer year;

  /** Carry-over annual leave days from previous year */
  @TableField("last_remaining_annual_leave_days")
  private BigDecimal lastRemainingAnnualLeaveDays;

  /** Annual leave days before anniversary date */
  @TableField("pre_effective_annual_leave_days")
  private BigDecimal preEffectiveAnnualLeaveDays;

  /** Anniversary date in this year */
  @TableField("effective_date")
  private LocalDate effectiveDate;

  /** Annual leave days after anniversary date */
  @TableField("post_effective_annual_leave_days")
  private BigDecimal postEffectiveAnnualLeaveDays;

  private String createBy;
  private java.time.LocalDateTime createAt;
  private String updateBy;
  private java.time.LocalDateTime updateAt;
  private Integer deleted;
}
