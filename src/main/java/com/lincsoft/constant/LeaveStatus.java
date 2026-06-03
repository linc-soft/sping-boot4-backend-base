package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Leave status enumeration.
 *
 * <p>Defines the possible statuses for a leave request.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Getter
@AllArgsConstructor
public enum LeaveStatus implements BaseEnum<Integer> {
  APPLYING(0, "Applying"),
  APPROVED(1, "Approved"),
  REJECTED(2, "Rejected");

  private final Integer code;
  private final String name;

  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
