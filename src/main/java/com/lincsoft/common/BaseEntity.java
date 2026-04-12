package com.lincsoft.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Base entity abstract class.
 *
 * <p>Defines common fields that all entities inherit.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
public abstract class BaseEntity implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /* ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /* Create User */
  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  /* Create Time */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createAt;

  /* Update User */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;

  /* Update Time */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateAt;
}
