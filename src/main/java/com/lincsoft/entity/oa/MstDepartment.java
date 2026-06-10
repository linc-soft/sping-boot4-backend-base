package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Department entity.
 *
 * <p>Multi-level department tree. {@code parentId} is {@code 0} for top-level departments. {@code
 * leaderUserId} references {@link MstUser#getId()} and is used by the approval workflow for
 * routing.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_department")
public class MstDepartment extends VersionedEntity {
  /** Department name */
  private String deptName;

  /** Department code (business identifier) */
  private String deptCode;

  /** Parent department ID (0 = top level) */
  private Long parentId;

  /** Department head (mst_user.id) */
  private Long leaderUserId;

  /** Sort order among siblings */
  private Integer sortOrder;

  /** Status (0 disabled / 1 enabled) */
  private String status;
}
