package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User Role entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_user_role")
public class MstUserRole extends BaseEntity {
  /** User ID */
  private Long userId;

  /** Role ID */
  private Long roleId;
}
