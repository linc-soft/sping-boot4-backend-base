package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Sex type enumeration.
 *
 * <p>Defines gender options for employees.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Getter
@AllArgsConstructor
public enum SexType implements BaseEnum<Integer> {
  MALE(0, "Male"),
  FEMALE(1, "Female");

  private final Integer code;
  private final String name;

  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
