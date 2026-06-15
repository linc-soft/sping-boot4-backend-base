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
  ADMIN("ADMIN", "common.enums.role-code.admin"),
  ROLE_READ("ROLE_READ", "common.enums.role-code.role-read"),
  ROLE_WRITE("ROLE_WRITE", "common.enums.role-code.role-write"),
  ROLE_DELETE("ROLE_DELETE", "common.enums.role-code.role-delete"),
  ROLE_EXPORT("ROLE_EXPORT", "common.enums.role-code.role-export"),
  USER_READ("USER_READ", "common.enums.role-code.user-read"),
  USER_WRITE("USER_WRITE", "common.enums.role-code.user-write"),
  USER_DELETE("USER_DELETE", "common.enums.role-code.user-delete"),
  USER_EXPORT("USER_EXPORT", "common.enums.role-code.user-export"),
  LOG_READ("LOG_READ", "common.enums.role-code.log-read"),
  LOG_EXPORT("LOG_EXPORT", "common.enums.role-code.log-export"),
  FILE_READ("FILE_READ", "common.enums.role-code.file-read"),
  FILE_WRITE("FILE_WRITE", "common.enums.role-code.file-write"),
  FILE_DELETE("FILE_DELETE", "common.enums.role-code.file-delete"),
  DEPT_READ("DEPT_READ", "common.enums.role-code.dept-read"),
  DEPT_WRITE("DEPT_WRITE", "common.enums.role-code.dept-write"),
  DEPT_DELETE("DEPT_DELETE", "common.enums.role-code.dept-delete"),
  POSITION_READ("POSITION_READ", "common.enums.role-code.position-read"),
  POSITION_WRITE("POSITION_WRITE", "common.enums.role-code.position-write"),
  POSITION_DELETE("POSITION_DELETE", "common.enums.role-code.position-delete");

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
