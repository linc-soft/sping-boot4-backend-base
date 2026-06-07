package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Leave type enumeration.
 *
 * <p>Defines the supported leave types. The code is stored in {@code oa_leave_request.leave_type}.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Getter
@AllArgsConstructor
public enum LeaveTypeEnum implements BaseEnum<String> {
  ANNUAL("1", "Annual Leave"),
  SICK("2", "Sick Leave"),
  PERSONAL("3", "Personal Leave"),
  MARRIAGE("4", "Marriage Leave"),
  MATERNITY("5", "Maternity Leave"),
  OTHER("9", "Other Leave");

  /** Leave type code. */
  private final String code;

  /** Leave type name. */
  private final String name;

  /** List of all valid leave type codes (cached). */
  private static final List<String> VALID_CODES =
      Arrays.stream(values()).map(LeaveTypeEnum::getCode).toList();

  /** Cached list of all leave type entries for API responses. */
  private static final List<Map<String, Object>> LEAVE_TYPE_LIST =
      BaseEnum.toList(LeaveTypeEnum.values());

  /**
   * Check whether the given code is a valid leave type.
   *
   * @param code the code to check
   * @return true if the code matches a defined leave type
   */
  public static boolean isValid(String code) {
    return VALID_CODES.contains(code);
  }

  /**
   * Get the list of all leave type entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LEAVE_TYPE_LIST;
  }
}
