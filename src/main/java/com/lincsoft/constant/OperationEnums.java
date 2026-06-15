package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Operation type enum, used to identify operation types in operation logs.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Getter
@AllArgsConstructor
public enum OperationEnums implements BaseEnum<String> {
  QUERY("QUERY", "common.enums.operation-type.query"),
  CREATE("CREATE", "common.enums.operation-type.create"),
  UPDATE("UPDATE", "common.enums.operation-type.update"),
  DELETE("DELETE", "common.enums.operation-type.delete"),
  LOGIN("LOGIN", "common.enums.operation-type.login"),
  LOGOUT("LOGOUT", "common.enums.operation-type.logout"),
  IMPORT("IMPORT", "common.enums.operation-type.import"),
  EXPORT("EXPORT", "common.enums.operation-type.export"),
  OTHER("OTHER", "common.enums.operation-type.other");

  /** Operation type code. */
  private final String code;

  /** Operation type display name. */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all operation types.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
