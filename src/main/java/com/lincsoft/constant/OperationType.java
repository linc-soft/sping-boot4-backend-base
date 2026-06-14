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
public enum OperationType implements BaseEnum<String> {
  QUERY("QUERY", "Query"),
  CREATE("CREATE", "Create"),
  UPDATE("UPDATE", "Update"),
  DELETE("DELETE", "Delete"),
  LOGIN("LOGIN", "Login"),
  LOGOUT("LOGOUT", "Logout"),
  IMPORT("IMPORT", "Import"),
  EXPORT("EXPORT", "Export"),
  OTHER("OTHER", "Other");

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
