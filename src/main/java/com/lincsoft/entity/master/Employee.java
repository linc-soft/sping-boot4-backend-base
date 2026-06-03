package com.lincsoft.entity.master;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee entity extending mst_user with business information.
 *
 * <p>1:1 relationship with mst_user via userId field.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_employee")
public class Employee extends BaseEntity {

  /** FK to mst_user.id */
  @TableField("user_id")
  private Long userId;

  /** Display name */
  private String nickname;

  /** Remark */
  private String remark;

  /** Mobile phone */
  private String mobile;

  /** Gender (0=Male, 1=Female) */
  private Integer sex;

  /** Hire date */
  private LocalDate hiredDate;
}
