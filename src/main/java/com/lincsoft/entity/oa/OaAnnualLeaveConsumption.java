package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Annual leave consumption ledger entity.
 *
 * <p>One row per grant batch affected by a leave request's FIFO consumption. Used to refund the
 * exact batches (by the exact days) when the leave request is rejected or withdrawn.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("oa_annual_leave_consumption")
public class OaAnnualLeaveConsumption extends BaseEntity {
  /** Leave request ID (oa_leave_request.id) */
  private Long leaveRequestId;

  /** Grant batch ID (oa_annual_leave_grant.id) */
  private Long grantId;

  /** Days consumed from this batch */
  private BigDecimal days;
}
