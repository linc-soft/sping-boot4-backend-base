package com.lincsoft.constant;

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
public enum OperationType {
  QUERY("QUERY"),
  CREATE("CREATE"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  LOGIN("LOGIN"),
  LOGOUT("LOGOUT"),
  IMPORT("IMPORT"),
  EXPORT("EXPORT"),
  OTHER("OTHER");

  private final String value;
}
