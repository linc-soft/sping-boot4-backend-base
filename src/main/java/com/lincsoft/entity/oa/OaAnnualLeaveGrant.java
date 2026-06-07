package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Annual leave grant entity.
 *
 * <p>One row per grant batch. A batch is granted on each employment anniversary and is valid for 24
 * months from {@code grantDate}. Consumption is FIFO (earliest {@code grantDate} first). {@code
 * grantedDays} is locked at grant time based on the tenure tier (5 / 7 / 9 days).
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("oa_annual_leave_grant")
public class OaAnnualLeaveGrant extends VersionedEntity {
  /** Employee ID (mst_employee.id) */
  private Long employeeId;

  /** Grant date (employment anniversary) */
  private LocalDate grantDate;

  /** Expiry date (grantDate + 24 months) */
  private LocalDate expireDate;

  /** Days granted in this batch (tenure tier 5/7/9) */
  private BigDecimal grantedDays;

  /** Days consumed from this batch */
  private BigDecimal usedDays;
}
