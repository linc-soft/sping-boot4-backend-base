package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_user")
public class MstUser extends VersionedEntity {
  /** Username */
  private String username;

  /** Password */
  private String password;

  /** Status */
  private String status;
}
