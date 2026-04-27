package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role Inheritance entity.
 *
 * <p>Represents a parent-child inheritance relationship between two roles. A child role inherits
 * all permissions from its parent roles (multi-inheritance supported).
 *
 * @author 林创科技
 * @since 2026-04-27
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_role_inheritance")
public class MstRoleInheritance extends BaseEntity {
  /** Child role ID (the role that inherits) */
  private Long childRoleId;

  /** Parent role ID (the role being inherited) */
  private Long parentRoleId;
}
