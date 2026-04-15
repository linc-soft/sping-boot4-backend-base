package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Row-level data permission grant entity.
 *
 * <p>Grants a specific subject (user, role, or department) access to a specific resource with the
 * given permission bits. Grants can be time-bounded via {@code validFrom} and {@code validUntil}.
 *
 * <p>The {@code permBits} field is a bitmask combining one or more {@code PermissionBit} values
 * (READ=1, WRITE=2, DELETE=4, EXPORT=8).
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_data_permission_grant")
public class MstDataPermissionGrant extends VersionedEntity {

  /** Resource type enum value (e.g. "ORDER", "CUSTOMER"). Stored as the ResourceType enum name. */
  private String resourceType;

  /** ID of the specific resource instance being granted access to. */
  private Long resourceId;

  /** Subject type: USER / ROLE / DEPT. Stored as the SubjectType enum name. */
  private String subjectType;

  /** ID of the subject (user_id, role_id, or dept_id depending on subjectType). */
  private Long subjectId;

  /** Permission bitmask: READ=1, WRITE=2, DELETE=4, EXPORT=8. Combined via bitwise OR. */
  private Integer permBits;

  /** Whether this grant propagates to child roles or child departments. */
  private Boolean inheritable;

  /** Start of the validity period. NULL means the grant is effective immediately. */
  private LocalDateTime validFrom;

  /** End of the validity period. NULL means the grant never expires. */
  private LocalDateTime validUntil;
}
