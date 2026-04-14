package com.lincsoft.common;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Versioned entity abstract class.
 *
 * @author 林创科技
 * @since 2026-04-14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class VersionedEntity extends BaseEntity {
  /* Version */
  @Version private Integer version;
}
