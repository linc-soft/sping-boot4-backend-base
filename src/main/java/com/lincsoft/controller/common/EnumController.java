package com.lincsoft.controller.common;

import com.lincsoft.constant.RoleCodeEnums;
import com.lincsoft.constant.UserStatusEnum;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enum controller for public enumeration data.
 *
 * <p>Provides endpoints for retrieving enumeration lists used by the frontend, such as user status
 * options. All endpoints are public and do not require authentication.
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@RestController
@RequestMapping("/api/common/enums")
public class EnumController {

  /**
   * Get enumeration list by type.
   *
   * @param type the enumeration type identifier (e.g., "user-status")
   * @return list of maps with "code" and "name" entries
   * @throws IllegalArgumentException if the type is not supported
   */
  @GetMapping
  public List<Map<String, Object>> getEnumList(@RequestParam String type) {
    return switch (type) {
      case "user-status" -> UserStatusEnum.getList();
      case "role-code" -> RoleCodeEnums.getList();
      default -> throw new IllegalArgumentException("Unsupported enum type: " + type);
    };
  }
}
