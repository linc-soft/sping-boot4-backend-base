package com.lincsoft.constant;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Role code enumeration class.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@AllArgsConstructor
public enum RoleCodeEnums {
  ADMIN("ADMIN", "Administrator"),
  USER("USER", "User");

  /** Role code. */
  @Getter private final String roleCode;

  /** Role description. */
  @Getter private final String description;

  /** List of all valid roleCodes (cached to avoid repeated calculations). */
  private static final List<String> VALID_CODES =
      Arrays.stream(values()).map(RoleCodeEnums::getRoleCode).toList();

  /**
   * Get the list of all valid roleCodes.
   *
   * @return list of roleCodes.
   */
  public static List<String> getValidCodes() {
    return VALID_CODES;
  }
}
