package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Department entity.
 *
 * <p>Represents a node in the department tree. The tree supports unlimited depth via the
 * self-referencing {@code parentId} field. A {@code null} parent ID indicates a root node.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_dept")
public class MstDept extends VersionedEntity {

  /** Department name. */
  private String deptName;

  /** Parent department ID. NULL indicates this is a root department. */
  private Long parentId;

  /** Sort order within the same level (ascending). */
  private Integer sortOrder;

  /** Status: '1' = active, '0' = disabled. */
  private String status;
}
