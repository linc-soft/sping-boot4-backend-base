package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * User status enumeration.
 *
 * <p>Defines the possible status values for user accounts.
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@Getter
@AllArgsConstructor
public enum UserStatusType implements BaseEnum<String> {
  ENABLED("1", "common.enums.user-status.enabled"),
  DISABLED("0", "common.enums.user-status.disabled"),
  INACTIVE("2", "common.enums.user-status.inactive");

  /** status code. */
  private final String code;

  /** status name. */
  private final String name;

  /** Cached list of all status entries for API responses. */
  private static final List<Map<String, Object>> STATUS_LIST =
      BaseEnum.toList(UserStatusType.values());

  /**
   * Get the list of all user status entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return STATUS_LIST;
  }
}
