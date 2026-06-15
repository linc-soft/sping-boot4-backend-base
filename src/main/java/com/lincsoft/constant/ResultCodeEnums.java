package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HTTP status code group enum for access log filtering.
 *
 * @author 林创科技
 * @since 2026-06-15
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnums implements BaseEnum<String> {
  SUCCESS("2xx", "common.enums.status-code.success"),
  CLIENT_ERROR("4xx", "common.enums.status-code.client-error"),
  SERVER_ERROR("5xx", "common.enums.status-code.server-error");

  /** Status code group. */
  private final String code;

  /** Status code group display name (i18n key). */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all status code group entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
