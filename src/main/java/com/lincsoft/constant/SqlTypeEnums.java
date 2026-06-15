package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SQL operation type enum for SQL logs.
 *
 * @author 林创科技
 * @since 2026-06-15
 */
@Getter
@AllArgsConstructor
public enum SqlTypeEnums implements BaseEnum<String> {
  SELECT("SELECT", "common.enums.sql-type.select"),
  INSERT("INSERT", "common.enums.sql-type.insert"),
  UPDATE("UPDATE", "common.enums.sql-type.update"),
  DELETE("DELETE", "common.enums.sql-type.delete");

  /** SQL type code. */
  private final String code;

  /** SQL type display name (i18n key). */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all SQL type entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
