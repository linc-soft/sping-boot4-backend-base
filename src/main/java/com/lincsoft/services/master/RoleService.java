package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstRoleInheritance;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstRoleInheritanceMapper;
import com.lincsoft.mapper.master.MstRoleMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  /** Role inheritance mapper for managing role hierarchy. */
  private final MstRoleInheritanceMapper roleInheritanceMapper;

  /** Self reference for lazy initialization. */
  @Lazy private final RoleService self;

  /**
   * Get roles by ID list.
   *
   * <p>Batch retrieves roles by their IDs. Used internally for role assignment operations.
   *
   * @param ids List of role IDs
   * @return List of roles matching the given IDs
   */
  public List<MstRole> getRolesByIds(List<Integer> ids) {
    return roleMapper.selectByIds(ids);
  }

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
      description = "Query roles by user ID: #{#userId}, return #{#result.size()} roles")
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
      description = "Query roles, return #{#result.size()} roles")
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
      description = "Query role #{#result.roleName} (#{#result.roleCode})")
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
      description = "Role created: #{#role.roleName} (#{#role.roleCode})")
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
      description = "Role updated: #{#role.roleName} (#{#role.roleCode})")
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
      description = "Role deleted: #{#role.roleName} (#{#role.roleCode})")
  public void deleteRole(Long id, Integer version) {
    // Get role for logging and validation
    MstRole role = self.getRoleById(id);

    // Check if role is in use
    validateRoleNotInUse(id);

    // Check if role has inheritance relationships
    validateRoleNotInherited(id);

    // Set version for optimistic locking
    role.setVersion(version);

    // Delete role (optimistic locking handled by @Version annotation)
    int deleted = roleMapper.deleteById(role);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "role");
    }
  }

  // ========== Role Inheritance Management ==========

  /**
   * Resolve all effective role codes for a given set of direct role IDs.
   *
   * <p>Uses BFS to traverse the inheritance graph, collecting all ancestor roles. Handles diamond
   * inheritance and prevents infinite loops via visited set.
   *
   * <p>Example: If role A inherits B, B inherits D, then resolving [A] returns codes of A, B, D.
   *
   * @param directRoleIds Direct role IDs assigned to the user
   * @return Set of all effective role codes (direct + inherited)
   */
  public Set<String> resolveAllRoleCodes(List<Long> directRoleIds) {
    if (directRoleIds == null || directRoleIds.isEmpty()) {
      return Set.of();
    }

    Set<Long> visited = new HashSet<>(directRoleIds);
    Queue<Long> queue = new LinkedList<>(directRoleIds);
    Set<String> roleCodes = new HashSet<>();

    while (!queue.isEmpty()) {
      Long currentId = queue.poll();
      MstRole role = roleMapper.selectById(currentId);
      if (role != null && role.getRoleCode() != null) {
        roleCodes.add(role.getRoleCode());
      }
      // Traverse parent roles
      List<Long> parentIds = getParentRoleIds(currentId);
      for (Long parentId : parentIds) {
        if (visited.add(parentId)) {
          queue.add(parentId);
        }
      }
    }
    return roleCodes;
  }

  /**
   * Add an inheritance relationship between two roles.
   *
   * <p>Validates that no circular dependency would be created and that the relationship does not
   * already exist (application-level duplicate check since UK cannot be used with soft delete).
   *
   * @param childRoleId Child role ID (the role that inherits)
   * @param parentRoleId Parent role ID (the role being inherited)
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.CREATE,
      description = "Added role inheritance: child=#{#childRoleId}, parent=#{#parentRoleId}")
  @Transactional(rollbackFor = Exception.class)
  public void addRoleInheritance(Long childRoleId, Long parentRoleId) {
    // Cannot inherit self
    if (childRoleId.equals(parentRoleId)) {
      throw new BusinessException(MessageEnums.CIRCULAR_DEPENDENCY, "role inheritance");
    }

    // Validate both roles exist
    self.getRoleById(childRoleId);
    self.getRoleById(parentRoleId);

    // Check for circular dependency: if parentRoleId's ancestors include childRoleId
    validateNoCircularDependency(childRoleId, parentRoleId);

    // Application-level duplicate check (soft delete prevents UK usage)
    validateInheritanceNotExists(childRoleId, parentRoleId);

    MstRoleInheritance inheritance = new MstRoleInheritance();
    inheritance.setChildRoleId(childRoleId);
    inheritance.setParentRoleId(parentRoleId);
    roleInheritanceMapper.insert(inheritance);
  }

  /**
   * Remove an inheritance relationship between two roles.
   *
   * @param childRoleId Child role ID
   * @param parentRoleId Parent role ID
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Manager",
      type = OperationType.DELETE,
      description = "Removed role inheritance: child=#{#childRoleId}, parent=#{#parentRoleId}")
  public void removeRoleInheritance(Long childRoleId, Long parentRoleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("child_role_id", childRoleId).eq("parent_role_id", parentRoleId);
    int deleted = roleInheritanceMapper.delete(qw);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "role inheritance");
    }
  }

  /**
   * Get parent roles for a given role ID.
   *
   * @param roleId Child role ID
   * @return List of parent MstRole entities
   */
  public List<MstRole> getParentRoles(Long roleId) {
    List<Long> parentIds = getParentRoleIds(roleId);
    if (parentIds.isEmpty()) {
      return List.of();
    }
    return roleMapper.selectByIds(parentIds);
  }

  /**
   * Get child roles for a given role ID.
   *
   * @param roleId Parent role ID
   * @return List of child MstRole entities
   */
  public List<MstRole> getChildRoles(Long roleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("parent_role_id", roleId);
    List<Long> childIds =
        roleInheritanceMapper.selectList(qw).stream()
            .map(MstRoleInheritance::getChildRoleId)
            .toList();
    if (childIds.isEmpty()) {
      return List.of();
    }
    return roleMapper.selectByIds(childIds);
  }

  // ========== Private Validation Methods ==========

  /**
   * Get parent role IDs for a given role (direct parents only).
   *
   * @param roleId Child role ID
   * @return List of direct parent role IDs
   */
  private List<Long> getParentRoleIds(Long roleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("child_role_id", roleId);
    return roleInheritanceMapper.selectList(qw).stream()
        .map(MstRoleInheritance::getParentRoleId)
        .toList();
  }

  /**
   * Validate that adding an inheritance relationship would not create a circular dependency.
   *
   * <p>Uses BFS to traverse all ancestors of the parent role. If the child role is found among
   * them, a circular dependency would be created.
   *
   * @param childRoleId The child role ID to be added
   * @param parentRoleId The parent role ID to be added
   * @throws BusinessException if a circular dependency would be created
   */
  private void validateNoCircularDependency(Long childRoleId, Long parentRoleId) {
    Set<Long> ancestors = new HashSet<>();
    Queue<Long> queue = new LinkedList<>();
    queue.add(parentRoleId);

    while (!queue.isEmpty()) {
      Long current = queue.poll();
      if (current.equals(childRoleId)) {
        throw new BusinessException(MessageEnums.CIRCULAR_DEPENDENCY, "role inheritance");
      }
      List<Long> parentIds = getParentRoleIds(current);
      for (Long parentId : parentIds) {
        if (ancestors.add(parentId)) {
          queue.add(parentId);
        }
      }
    }
  }

  /**
   * Validate that the inheritance relationship does not already exist.
   *
   * <p>Since the table uses soft delete (deleted flag), a UNIQUE KEY cannot be applied. This method
   * performs an application-level duplicate check.
   *
   * @param childRoleId Child role ID
   * @param parentRoleId Parent role ID
   * @throws BusinessException if the relationship already exists
   */
  private void validateInheritanceNotExists(Long childRoleId, Long parentRoleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("child_role_id", childRoleId).eq("parent_role_id", parentRoleId);
    if (roleInheritanceMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.UNIQUE_CONSTRAINT_VIOLATION, "role inheritance");
    }
  }

  /**
   * Validate that the role has no inheritance relationships (neither as parent nor child).
   *
   * @param roleId Role ID to check
   * @throws BusinessException if the role has any inheritance relationship
   */
  private void validateRoleNotInherited(Long roleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("parent_role_id", roleId).or().eq("child_role_id", roleId);
    long count = roleInheritanceMapper.selectCount(qw);
    if (count > 0) {
      throw new BusinessException(
          MessageEnums.RESOURCE_IS_USED, "role (has inheritance relationships)");
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
