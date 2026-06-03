package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Leave type enumeration.
 *
 * <p>Defines the types of leave requests.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Getter
@AllArgsConstructor
public enum LeaveType implements BaseEnum<Integer> {
  ANNUAL(0, "Annual Leave"),
  PERSONAL(1, "Personal Leave"),
  SICK(2, "Sick Leave"),
  MARRIAGE(3, "Marriage Leave"),
  PATERNITY(4, "Paternity/Maternity Leave"),
  BEREAVEMENT(5, "Bereavement Leave");

  private final Integer code;
  private final String name;

  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
