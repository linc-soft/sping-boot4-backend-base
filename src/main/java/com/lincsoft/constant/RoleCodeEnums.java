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
  LIST_OPTIONS("LIST_OPTIONS", "common.enums.role-code.list-options"),

  LIST_ROLE("LIST_ROLE", "common.enums.role-code.list-role"),
  VIEW_ROLE("VIEW_ROLE", "common.enums.role-code.view-role"),
  CREATE_ROLE("CREATE_ROLE", "common.enums.role-code.create-role"),
  UPDATE_ROLE("UPDATE_ROLE", "common.enums.role-code.update-role"),
  DELETE_ROLE("DELETE_ROLE", "common.enums.role-code.delete-role"),

  LIST_USER("LIST_USER", "common.enums.role-code.list-user"),
  VIEW_USER("VIEW_USER", "common.enums.role-code.view-user"),
  CREATE_USER("CREATE_USER", "common.enums.role-code.create-user"),
  UPDATE_USER("UPDATE_USER", "common.enums.role-code.update-user"),
  DELETE_USER("DELETE_USER", "common.enums.role-code.delete-user"),
  EXPORT_USER("EXPORT_USER", "common.enums.role-code.export-user"),

  LIST_DEPARTMENT("LIST_DEPARTMENT", "common.enums.role-code.list-department"),
  VIEW_DEPARTMENT("VIEW_DEPARTMENT", "common.enums.role-code.view-department"),
  CREATE_DEPARTMENT("CREATE_DEPARTMENT", "common.enums.role-code.create-department"),
  UPDATE_DEPARTMENT("UPDATE_DEPARTMENT", "common.enums.role-code.update-department"),
  DELETE_DEPARTMENT("DELETE_DEPARTMENT", "common.enums.role-code.delete-department"),

  LIST_POSITION("LIST_POSITION", "common.enums.role-code.list-position"),
  VIEW_POSITION("VIEW_POSITION", "common.enums.role-code.view-position"),
  CREATE_POSITION("CREATE_POSITION", "common.enums.role-code.create-position"),
  UPDATE_POSITION("UPDATE_POSITION", "common.enums.role-code.update-position"),
  DELETE_POSITION("DELETE_POSITION", "common.enums.role-code.delete-position"),

  LIST_RESOURCE("LIST_RESOURCE", "common.enums.role-code.list-resource"),
  VIEW_RESOURCE("VIEW_RESOURCE", "common.enums.role-code.view-resource"),
  UPDATE_RESOURCE("UPDATE_RESOURCE", "common.enums.role-code.update-resource"),

  LIST_LOG("LIST_LOG", "common.enums.role-code.list-log"),
  VIEW_LOG("VIEW_LOG", "common.enums.role-code.view-log"),
  EXPORT_LOG("EXPORT_LOG", "common.enums.role-code.export-log"),

  UPLOAD_FILE("UPLOAD_FILE", "common.enums.role-code.upload-file"),
  DOWNLOAD_FILE("DOWNLOAD_FILE", "common.enums.role-code.download-file"),
  REMOVE_FILE("REMOVE_FILE", "common.enums.role-code.remove-file");

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
