package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Module enumeration for operation log module identification.
 *
 * <p>Defines the identifier codes and display names for business modules in the system.
 *
 * @author 林创科技
 * @since 2026-05-12
 */
@Getter
@AllArgsConstructor
public enum Module implements BaseEnum<String> {
  MASTER("MASTER", "Master"),
  AUTH("AUTH", "Auth"),
  SYSTEM("SYSTEM", "System"),
  LEAVE("LEAVE", "Leave");

  /** Module code. */
  private final String code;

  /** Module display name. */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all modules.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
