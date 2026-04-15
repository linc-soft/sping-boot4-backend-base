package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstRoleMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Role service.
 *
 * <p>Provides business logic for role management.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class RoleService {
  /** Role mapper for database operations. */
  private final MstRoleMapper roleMapper;

  /** User role mapper for checking role usage. */
  private final MstUserRoleMapper userRoleMapper;

  /** Self reference for lazy initialization. */
  @Lazy private final RoleService self;

  /**
   * Get role list by user ID.
   *
   * @param userId User ID
   * @return List of roles
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.QUERY,
      description = "Query roles by user ID: #{userId}, return #{result.size()} roles")
  public List<MstRole> getRoleListByUserId(Long userId) {
    return roleMapper.selectJoinList(
        MstRole.class,
        new MPJLambdaWrapper<MstRole>()
            .selectAll(MstRole.class)
            .innerJoin(MstUserRole.class, MstUserRole::getRoleId, MstRole::getId)
            .eq(MstUserRole::getUserId, userId));
  }

  /**
   * Get role list by query conditions.
   *
   * @param roleName Role name (partial match)
   * @param roleCode Role code (prefix match)
   * @param description Description (partial match)
   * @return List of roles
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.QUERY,
      description = "Query roles, return #{result.size()} roles")
  public List<MstRole> getRoleList(String roleName, String roleCode, String description) {
    QueryWrapper<MstRole> queryWrapper = new QueryWrapper<>();

    // Partial match for role name
    if (roleName != null && !roleName.isBlank()) {
      queryWrapper.like("role_name", roleName);
    }

    // Prefix match for role code
    if (roleCode != null && !roleCode.isBlank()) {
      queryWrapper.likeRight("role_code", roleCode);
    }

    // Partial match for description
    if (description != null && !description.isBlank()) {
      queryWrapper.like("description", description);
    }

    // Order by update time descending
    queryWrapper.orderByDesc("update_at");

    return roleMapper.selectList(queryWrapper);
  }

  /**
   * Get role by ID.
   *
   * @param id Role ID
   * @return MstRole entity
   * @throws BusinessException if the role is not found
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.QUERY,
      description = "Query role #{result.roleName} (#{result.roleCode})")
  public MstRole getRoleById(Long id) {
    MstRole role = roleMapper.selectById(id);
    if (role == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "role");
    }
    return role;
  }

  /**
   * Create a new role.
   *
   * <p>Checks for role code uniqueness before inserting. Throws an exception if the role code
   * already exists.
   *
   * @param role MstRole entity
   * @return The created role ID
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.CREATE,
      description = "Role created: #{role.roleName} (#{role.roleCode})")
  public Long createRole(MstRole role) {
    // Validate role code uniqueness (excluding any existing role)
    validateRoleCodeUnique(role.getRoleCode(), null);

    // Insert role
    roleMapper.insert(role);

    return role.getId();
  }

  /**
   * Update an existing role.
   *
   * <p>Checks for role code uniqueness (excluding the current role) before updating. Uses
   * optimistic locking via version field. Throws an exception if the role code already exists or if
   * the role was modified by another transaction.
   *
   * @param role MstRole entity with updated values
   * @throws BusinessException if the role code is duplicate or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.UPDATE,
      description = "Role updated: #{role.roleName} (#{role.roleCode})")
  public void updateRole(MstRole role) {
    // Validate role code uniqueness (excluding current role)
    validateRoleCodeUnique(role.getRoleCode(), role.getId());

    // Update role (optimistic locking handled by @Version annotation)
    int updated = roleMapper.updateById(role);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "role");
    }
  }

  /**
   * Delete a role.
   *
   * <p>Checks if the role is in use by any user before deleting. Uses optimistic locking via
   * version field. Throws an exception if the role is in use or if the role was modified by another
   * transaction.
   *
   * @param id Role ID
   * @param version Version for optimistic locking
   * @throws BusinessException if the role is in use, not found, or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.DELETE,
      description = "Role deleted: #{role.roleName} (#{role.roleCode})")
  public void deleteRole(Long id, Integer version) {
    // Get role for logging and validation
    MstRole role = self.getRoleById(id);

    // Check if role is in use
    validateRoleNotInUse(id);

    // Set version for optimistic locking
    role.setVersion(version);

    // Delete role (optimistic locking handled by @Version annotation)
    int deleted = roleMapper.deleteById(role);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "role");
    }
  }

  /**
   * Validate that the role is not in use by any user.
   *
   * @param roleId Role ID to check
   * @throws BusinessException if the role is assigned to any user
   */
  private void validateRoleNotInUse(Long roleId) {
    QueryWrapper<MstUserRole> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("role_id", roleId);
    long count = userRoleMapper.selectCount(queryWrapper);
    if (count > 0) {
      throw new BusinessException(MessageEnums.RESOURCE_IS_USED, "role");
    }
  }

  /**
   * Validate that the role code is unique.
   *
   * @param roleCode Role code to check
   * @throws BusinessException if the role code already exists
   */
  private void validateRoleCodeUnique(String roleCode, Long excludeId) {
    QueryWrapper<MstRole> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("role_code", roleCode);
    // Exclude the current role if an excludeId is provided
    if (excludeId != null) {
      queryWrapper.ne("id", excludeId);
    }
    if (roleMapper.selectCount(queryWrapper) > 0) {
      throw new BusinessException(MessageEnums.UNIQUE_CONSTRAINT_VIOLATION, "role code");
    }
  }
}
