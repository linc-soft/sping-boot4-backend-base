package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Generic enabled/disabled status enum.
 *
 * <p>Shared by entities that use a simple {@code status} flag such as departments and positions.
 *
 * @author 林创科技
 * @since 2026-06-15
 */
@Getter
@AllArgsConstructor
public enum CommonStatusType implements BaseEnum<String> {
  ENABLED("1", "common.enums.status-type.enabled"),
  DISABLED("0", "common.enums.status-type.disabled");

  /** Status code. */
  private final String code;

  /** Status display name (i18n key). */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all status type entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
