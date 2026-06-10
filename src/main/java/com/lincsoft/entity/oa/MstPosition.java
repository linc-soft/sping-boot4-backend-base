package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Position entity.
 *
 * <p>Job position / title. Referenced by {@link MstUser#getPositionId()}.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_position")
public class MstPosition extends VersionedEntity {
  /** Position name */
  private String positionName;

  /** Position code (business identifier) */
  private String positionCode;

  /** Sort order */
  private Integer sortOrder;

  /** Status (0 disabled / 1 enabled) */
  private String status;
}
