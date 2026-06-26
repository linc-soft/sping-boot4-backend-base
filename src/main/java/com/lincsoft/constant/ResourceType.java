package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Resource type enum.
 *
 * <p>Defines the available frontend resource types used in the permission tree.
 *
 * @author 林创科技
 * @since 2026-06-18
 */
@Getter
@AllArgsConstructor
public enum ResourceType implements BaseEnum<String> {
  DIRECTORY("0", "common.enums.resource-type.directory"),
  PAGE("1", "common.enums.resource-type.page"),
  BUTTON("2", "common.enums.resource-type.button");

  /** Resource type code. */
  private final String code;

  /** Resource type display name (i18n key). */
  private final String name;

  /** Cached enum list for API responses. */
  private static final List<Map<String, Object>> LIST = BaseEnum.toList(values());

  /**
   * Get the list of all resource type entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return LIST;
  }
}
