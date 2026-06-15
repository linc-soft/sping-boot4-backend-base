package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Module type enumeration for operation log module identification.
 *
 * <p>Defines the identifier codes and i18n keys for business modules in the system.
 *
 * @author 林创科技
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public enum ModuleEnums implements BaseEnum<String> {
  MASTER("MASTER", "common.enums.module-type.master"),
  AUTH("AUTH", "common.enums.module-type.auth"),
  SYSTEM("SYSTEM", "common.enums.module-type.system");

  /** Module code. */
  private final String code;

  /** Module i18n key. */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all module types.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
