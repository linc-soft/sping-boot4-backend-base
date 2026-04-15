package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role entity
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_role")
public class MstRole extends VersionedEntity {
  /** Role Name */
  private String roleName;

  /** Role Code */
  private String roleCode;

  /** Role Description */
  private String description;

  /** Parent role ID for single inheritance. NULL indicates this is a root role. */
  private Long parentRoleId;
}
