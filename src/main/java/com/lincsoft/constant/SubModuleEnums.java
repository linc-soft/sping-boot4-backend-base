package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Sub-module type enumeration for operation log sub-module identification.
 *
 * <p>Defines the identifier codes and i18n keys for business sub-modules in the system.
 *
 * @author 林创科技
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public enum SubModuleEnums implements BaseEnum<String> {
  SESSION("SESSION", "common.enums.sub-module-type.session"),
  ROLE("ROLE", "common.enums.sub-module-type.role"),
  USER("USER", "common.enums.sub-module-type.user"),
  FILE("FILE", "common.enums.sub-module-type.file"),
  PERMISSION("PERMISSION", "common.enums.sub-module-type.permission"),
  DEPARTMENT("DEPARTMENT", "common.enums.sub-module-type.department"),
  POSITION("POSITION", "common.enums.sub-module-type.position");

  /** Sub-module code. */
  private final String code;

  /** Sub-module i18n key. */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all sub-module types.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
