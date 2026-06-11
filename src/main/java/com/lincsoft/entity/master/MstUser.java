package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import java.time.LocalDate;
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

  /** Email address */
  private String email;

  /** Status */
  private String status;

  /** Real name */
  private String realName;

  /** Department ID */
  private Long deptId;

  /** Position ID */
  private Long positionId;

  /** Mobile phone */
  private String mobile;

  /** Gender (0 unknown / 1 male / 2 female) */
  private String gender;

  /** Birthday */
  private LocalDate birthday;

  /** Position name (non-persistent, populated by JOIN query) */
  @TableField(exist = false)
  private String positionName;
}
