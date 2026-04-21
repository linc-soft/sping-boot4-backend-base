package com.lincsoft.controller.master;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.master.vo.UserCreateRequest;
import com.lincsoft.controller.master.vo.UserDeleteRequest;
import com.lincsoft.controller.master.vo.UserInfoResponse;
import com.lincsoft.controller.master.vo.UserListRequest;
import com.lincsoft.controller.master.vo.UserListResponseItem;
import com.lincsoft.controller.master.vo.UserPageRequest;
import com.lincsoft.controller.master.vo.UserPageResponseItem;
import com.lincsoft.controller.master.vo.UserUpdateRequest;
import com.lincsoft.mapstruct.UserMapper;
import com.lincsoft.services.master.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * User controller.
 *
 * <p>Provides endpoints for user CRUD operations.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  /** User service */
  private final UserService userService;

  /** User mapper for converting between VO and entity. */
  private final UserMapper userMapper;

  /**
   * Get user by ID.
   *
   * @param id User ID
   * @return User info response
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
  @GetMapping("/{id}")
  public UserInfoResponse getUser(@PathVariable Long id) {
    return userMapper.toInfoResponse(userService.getUserById(id));
  }

  /**
   * Get user list by query conditions.
   *
   * @param request Query parameters (username, status)
   * @return List of user items
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
  @GetMapping
  public List<UserListResponseItem> getUserList(UserListRequest request) {
    return userMapper.toListResponse(userService.getUserList(request.username(), request.status()));
  }

  /**
   * Get user page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of user items
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER.roleCode)")
  @GetMapping("/page")
  public IPage<UserPageResponseItem> getUserPage(UserPageRequest request) {
    return userMapper.toPageResponse(userService.getUserPage(request));
  }

  /**
   * Create a new user.
   *
   * @param request User create request
   * @return created user ID
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  @PostMapping
  public Long createUser(@Valid @RequestBody UserCreateRequest request) {
    return userService.createUser(userMapper.toEntity(request), request.roleIds());
  }

  /**
   * Update an existing user.
   *
   * @param request User update request
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  @PutMapping
  public void updateUser(@Valid @RequestBody UserUpdateRequest request) {
    userService.updateUser(userMapper.toEntity(request));
  }

  /**
   * Delete a user.
   *
   * @param request User delete request
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  @DeleteMapping
  public void deleteUser(@Valid @RequestBody UserDeleteRequest request) {
    userService.deleteUser(request.id(), request.version());
  }
}
