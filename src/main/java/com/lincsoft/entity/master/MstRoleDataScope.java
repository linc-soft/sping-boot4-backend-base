package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Role data scope configuration entity.
 *
 * <p>Defines the department-based data access scope for a role. The {@code scopeType} field
 * determines the scope strategy:
 *
 * <ul>
 *   <li>{@code ALL} – no department restriction
 *   <li>{@code DEPT} – restricted to the specified {@code deptId}
 *   <li>{@code DEPT_AND_CHILD} – restricted to the specified {@code deptId} and all its descendants
 * </ul>
 *
 * <p>The {@code permBits} field is a bitmask combining one or more {@code PermissionBit} values
 * (READ=1, WRITE=2, DELETE=4, EXPORT=8).
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_role_data_scope")
public class MstRoleDataScope extends VersionedEntity {

  /** Role ID that owns this data scope configuration. */
  private Long roleId;

  /** Scope type: ALL / DEPT / DEPT_AND_CHILD. Stored as the enum name string. */
  private String scopeType;

  /** Target department ID. Required when scopeType is DEPT or DEPT_AND_CHILD; NULL for ALL. */
  private Long deptId;

  /** Permission bitmask: READ=1, WRITE=2, DELETE=4, EXPORT=8. Combined via bitwise OR. */
  private Integer permBits;

  /**
   * Whether this scope configuration is active. Disabled scopes are excluded from permission
   * resolution.
   */
  private Boolean enabled;

  /** Whether child roles can inherit this scope configuration. */
  private Boolean inheritable;
}
