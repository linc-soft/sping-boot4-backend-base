package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Submodule enumeration for operation log sub-module identification.
 *
 * <p>Defines the identifier codes and display names for business sub-modules in the system.
 *
 * @author 林创科技
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public enum SubModule implements BaseEnum<String> {
  ROLE_MANAGER("ROLE_MANAGER", "Role Manager"),
  USER_MANAGER("USER_MANAGER", "User Manager"),
  PERMISSION("PERMISSION", "Permission");

  /** Sub-module code. */
  private final String code;

  /** Sub-module display name. */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all submodules.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
