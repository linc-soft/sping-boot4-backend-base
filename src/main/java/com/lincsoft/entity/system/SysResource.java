package com.lincsoft.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lincsoft.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * System Resource entity.
 *
 * <p>Represents a frontend permission resource (directory / page / button) in a tree structure.
 * Visibility is determined by comparing {@code roleCode} against the user's resolved role codes.
 *
 * @author 林创科技
 * @since 2026-06-17
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_resource")
public class SysResource extends BaseEntity {
  /** Business code (unique, e.g. user:read) */
  private String resourceCode;

  /** i18n key for display name */
  private String resourceName;

  /** 0=directory, 1=page, 2=button */
  private Integer type;

  /** Parent resource ID (0 = top level) */
  private Long parentId;

  /** Route path (type=1 only) */
  private String routePath;

  /** Menu icon (mdi-*) */
  private String icon;

  /** Sort order among siblings */
  private Integer sortOrder;

  /** Role code for visibility (NULL for directories) */
  private String roleCode;

  /** Status (0 disabled / 1 enabled) */
  private String status;

  /** Optimistic lock version */
  @Version private Integer version;
}
