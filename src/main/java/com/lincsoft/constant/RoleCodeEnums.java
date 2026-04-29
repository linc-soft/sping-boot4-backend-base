package com.lincsoft.constant;

import com.lincsoft.common.BaseEnum;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Role code enumeration class.
 *
 * <p>Defines the available role codes and their display names. Implements {@link BaseEnum} with
 * String-typed code to support unified enumeration listing.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Getter
@AllArgsConstructor
public enum RoleCodeEnums implements BaseEnum<String> {
  ADMIN("ADMIN", "Administrator"),
  ROLE_VIEW("ROLE_VIEW", "Role Viewer"),
  ROLE_NEW("ROLE_NEW", "Role Creator"),
  ROLE_EDIT("ROLE_EDIT", "Role Editor"),
  ROLE_DEL("ROLE_DEL", "Role Deleter"),
  USER_VIEW("USER_VIEW", "User Viewer"),
  USER_NEW("USER_NEW", "User Creator"),
  USER_EDIT("USER_EDIT", "User Editor"),
  USER_DEL("USER_DEL", "User Deleter");

  /** Role code (also serves as the BaseEnum code). */
  private final String roleCode;

  /** Role display name. */
  private final String name;

  /**
   * Get the code of this role enum.
   *
   * @return the role code string
   */
  @Override
  public String getCode() {
    return roleCode;
  }

  /** List of all valid roleCodes (cached to avoid repeated calculations). */
  private static final List<String> VALID_CODES =
      Arrays.stream(values()).map(RoleCodeEnums::getRoleCode).toList();

  /** Cached list of all role entries for API responses. */
  private static final List<Map<String, Object>> ROLE_LIST =
      BaseEnum.toList(RoleCodeEnums.values());

  /**
   * Get the list of all valid roleCodes.
   *
   * @return list of roleCodes.
   */
  public static List<String> getValidCodes() {
    return VALID_CODES;
  }

  /**
   * Get the list of all role entries.
   *
   * @return list of maps with "code" and "name" entries
   */
  public static List<Map<String, Object>> getList() {
    return ROLE_LIST;
  }
}
