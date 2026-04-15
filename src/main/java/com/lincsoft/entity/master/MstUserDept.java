package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User-department relationship entity.
 *
 * <p>Records the many-to-many association between users and departments. A user may belong to
 * multiple departments, and a department may contain multiple users.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_user_dept")
public class MstUserDept extends BaseEntity {

  /** User ID. */
  private Long userId;

  /** Department ID. */
  private Long deptId;
}
