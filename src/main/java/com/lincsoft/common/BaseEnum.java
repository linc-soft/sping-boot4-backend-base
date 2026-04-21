package com.lincsoft.common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Base interface for enumerations with code and name properties.
 *
 * <p>Provides a common contract for enumerations that carry a typed code and a descriptive name,
 * along with a utility method to convert enum values into a serializable list format.
 *
 * @param <T> the type of the enum code (e.g., Integer, String)
 * @author 林创科技
 * @since 2026-04-21
 */
public interface BaseEnum<T> {

  /**
   * Get the code of the enum value.
   *
   * @return the code
   */
  T getCode();

  /**
   * Get the descriptive name of the enum value.
   *
   * @return the name
   */
  String getName();

  /**
   * Convert all values of a BaseEnum implementation to a list of maps containing code and name.
   *
   * @param values the enum values array
   * @param <T> the type of the enum code
   * @param <E> the enum type that implements BaseEnum
   * @return list of maps with "code" and "name" entries
   */
  static <T, E extends BaseEnum<T>> List<Map<String, Object>> toList(E[] values) {
    return Arrays.stream(values)
        .map(e -> Map.of("code", e.getCode(), "name", e.getName()))
        .toList();
  }
}
