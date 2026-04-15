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
import java.util.List;
import lombok.RequiredArgsConstructor;
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

  /**
   * Get role list by user ID.
   *
   * @param userId User ID
   * @return List of roles
   */
  public List<MstRole> getRoleListByUserId(Long userId) {
    return roleMapper.selectJoinList(
        MstRole.class,
        new MPJLambdaWrapper<MstRole>()
            .selectAll(MstRole.class)
            .innerJoin(MstUserRole.class, MstUserRole::getRoleId, MstRole::getId)
            .eq(MstUserRole::getUserId, userId));
  }

  /**
   * Get role by ID.
   *
   * @param id Role ID
   * @return MstRole entity
   * @throws BusinessException if the role is not found
   */
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
