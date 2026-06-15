package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Gender enum for users and employees.
 *
 * @author 林创科技
 * @since 2026-06-15
 */
@Getter
@AllArgsConstructor
public enum GenderType implements BaseEnum<String> {
  UNKNOWN("0", "common.enums.gender-type.unknown"),
  MALE("1", "common.enums.gender-type.male"),
  FEMALE("2", "common.enums.gender-type.female");

  /** Gender code. */
  private final String code;

  /** Gender display name (i18n key). */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all gender type entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
