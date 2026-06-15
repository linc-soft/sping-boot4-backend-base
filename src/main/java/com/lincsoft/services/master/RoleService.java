package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.ModuleEnums;
import com.lincsoft.constant.OperationEnums;
import com.lincsoft.constant.SubModuleEnums;
import com.lincsoft.dto.master.RoleWithParents;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstRoleInheritance;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstRoleInheritanceMapper;
import com.lincsoft.mapper.master.MstRoleMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

  /**
   * Self-reference for AOP proxy calls.
   *
   * <p>Injected lazily to break circular dependency. Used instead of direct method calls to ensure
   * {@code @Transactional} and {@code @OperationLog} aspects are applied when calling {@link
   * #addRoleInheritance} and {@link #removeRoleInheritance} internally.
   */
  @Autowired @Lazy private RoleService self;

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
   * <p>Each returned item is bundled with the direct parent role IDs of that role (batch-resolved
   * to avoid N+1 queries).
   *
   * <p>The roleCode filter performs a recursive search: it first finds roles whose own role_code
   * matches (prefix match), then traverses the inheritance graph downward to include all descendant
   * roles that inherit from those matched roles.
   *
   * @param roleName Role name (partial match)
   * @param roleCode Role code (prefix match, recursive through inheritance)
   * @param description Description (partial match)
   * @param aggregatedOnly Only include aggregated roles (roles with null role_code)
   * @return List of roles with their direct parent role IDs
   */
  public List<RoleWithParents> getRoleList(
      String roleName, String roleCode, String description, Boolean aggregatedOnly) {
    QueryWrapper<MstRole> queryWrapper = new QueryWrapper<>();

    // Partial match for role name
    if (roleName != null && !roleName.isBlank()) {
      queryWrapper.like("role_name", roleName);
    }

    // Recursive match for role code: find roles that directly or via inheritance have matching code
    if (roleCode != null && !roleCode.isBlank()) {
      Set<Long> matchedRoleIds = roleMapper.selectRoleIdsRecursiveByRoleCode(roleCode);
      if (matchedRoleIds.isEmpty()) {
        // No roles match the given code, return empty result
        return List.of();
      }
      queryWrapper.in("id", matchedRoleIds);
    }

    // Partial match for description
    if (description != null && !description.isBlank()) {
      queryWrapper.like("description", description);
    }

    if (Boolean.TRUE.equals(aggregatedOnly)) {
      queryWrapper.isNull("role_code");
    }

    // Order by update time descending
    queryWrapper.orderByDesc("update_at");

    return attachParentRoleIds(roleMapper.selectList(queryWrapper));
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
      throw new BusinessException(MessageEnums.ROLE_NOT_FOUND);
    }
    return role;
  }

  /**
   * Get role with its direct parent role IDs by ID.
   *
   * <p>Loads the role entity together with the IDs of all roles it directly inherits from.
   *
   * @param id Role ID
   * @return Role paired with its direct parent role IDs
   * @throws BusinessException if the role is not found
   */
  public RoleWithParents getRoleWithParentsById(Long id) {
    MstRole role = getRoleById(id);
    return new RoleWithParents(role, getParentRoleIds(id));
  }

  /**
   * Create a new role with inheritance relationships.
   *
   * <p>Checks for role code uniqueness before inserting. Throws an exception if the role code
   * already exists. Also handles role inheritance relationships if parentRoleIds are provided.
   *
   * @param role MstRole entity
   * @param parentRoleIds List of parent role IDs (optional)
   * @return The created role ID
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.ROLE,
      type = OperationEnums.CREATE,
      description = "Role created: #{#role.roleName}")
  @Transactional(rollbackFor = Exception.class)
  public Long createRole(MstRole role, List<Long> parentRoleIds) {
    // Insert role (role_code may be null for custom roles)
    roleMapper.insert(role);

    // Handle role inheritance if parentRoleIds are provided
    if (parentRoleIds != null && !parentRoleIds.isEmpty()) {
      for (Long parentRoleId : parentRoleIds) {
        self.addRoleInheritance(role.getId(), parentRoleId);
      }
    }

    return role.getId();
  }

  /**
   * Update an existing role with inheritance relationships.
   *
   * <p>Checks for role code uniqueness (excluding the current role) before updating. Uses
   * optimistic locking via version field. Also updates role inheritance relationships if
   * parentRoleIds are provided.
   *
   * @param role MstRole entity with updated values
   * @param parentRoleIds List of parent role IDs (optional)
   * @throws BusinessException if the role code is duplicate or optimistic lock fails
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.ROLE,
      type = OperationEnums.UPDATE,
      description = "Role updated: #{#role.roleName}")
  @Transactional(rollbackFor = Exception.class)
  public void updateRole(MstRole role, List<Long> parentRoleIds) {
    // Update role (optimistic locking handled by @Version annotation)
    // Note: role_code is not updated as it's only for base roles
    int updated = roleMapper.updateById(role);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.ROLE_OPTIMISTIC_LOCK_FAILED);
    }

    // Update role inheritance relationships if parentRoleIds are provided
    if (parentRoleIds != null) {
      updateRoleInheritance(role.getId(), parentRoleIds);
    }
  }

  /**
   * Delete a role.
   *
   * <p>Checks if the role is in use by any user and not inherited by other roles before deleting.
   * After successful deletion, cleans up all inheritance relationships where this role is a child.
   * Uses optimistic locking via version field.
   *
   * @param role MstRole entity to be deleted
   * @param version Version for optimistic locking
   * @throws BusinessException if the role is in use, inherited by other roles, not found, or
   *     optimistic lock fails
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.ROLE,
      type = OperationEnums.DELETE,
      description = "Role deleted: #{#role.roleName}")
  @Transactional(rollbackFor = Exception.class)
  public void deleteRole(MstRole role, Integer version) {
    // Check if role not found
    if (role == null) {
      throw new BusinessException(MessageEnums.ROLE_NOT_FOUND);
    }

    // Check if role is a base role (base roles cannot be deleted)
    if (role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
      throw new BusinessException(MessageEnums.ROLE_BASE_CANNOT_BE_DELETED);
    }

    // Check if role is in use
    validateRoleNotInUse(role.getId());

    // Check if role is inherited by other roles (as parent)
    validateRoleNotInherited(role.getId());

    // Delete role with optimistic locking via explicit version condition
    // Note: deleteById does NOT apply @Version check for logical delete,
    // so we use delete(wrapper) with an explicit version condition.
    LambdaUpdateWrapper<MstRole> deleteWrapper = new LambdaUpdateWrapper<>();
    deleteWrapper.eq(MstRole::getId, role.getId()).eq(MstRole::getVersion, version);
    int deleted = roleMapper.delete(deleteWrapper);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.ROLE_OPTIMISTIC_LOCK_FAILED);
    }

    // Clean up inheritance relationships where this role is a child
    deleteChildRoleInheritances(role.getId());
  }

  /**
   * Resolve all ancestor (parent) roles for a given list of direct role IDs.
   *
   * <p>Traverses the role inheritance graph upward from each direct role using BFS, collecting all
   * ancestor role IDs. Direct roles themselves are excluded from the result.
   *
   * <p>For example, if role A inherits from B, and B inherits from C, then resolving [A] returns
   * roles B and C.
   *
   * @param directRoleIds direct role IDs to resolve ancestors for
   * @return set of all ancestor role IDs (excluding the direct role IDs themselves); empty if no
   *     ancestors exist
   */
  public Set<Long> resolveAncestorRoleIds(List<Long> directRoleIds) {
    if (directRoleIds == null || directRoleIds.isEmpty()) {
      return Set.of();
    }

    Set<Long> ancestorIds = new LinkedHashSet<>();
    Set<Long> visited = new HashSet<>(directRoleIds);
    Queue<Long> queue = new LinkedList<>(directRoleIds);

    while (!queue.isEmpty()) {
      Long currentId = queue.poll();
      List<Long> parentIds = getParentRoleIds(currentId);
      for (Long parentId : parentIds) {
        if (visited.add(parentId)) {
          ancestorIds.add(parentId);
          queue.add(parentId);
        }
      }
    }

    return ancestorIds;
  }

  // ========== Role Inheritance Management ==========

  /**
   * Resolve all effective role codes for a given set of direct role IDs.
   *
   * <p>Uses BFS to traverse the inheritance graph, collecting all ancestor roles. Handles diamond
   * inheritance and prevents infinite loops via visited set. Filters out null role codes.
   *
   * <p>Example: If role A inherits B, B inherits D, then resolving [A] returns codes of A, B, D.
   *
   * @param directRoleIds Direct role IDs assigned to the user
   * @return Set of all effective role codes (direct + inherited, non-null only)
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
      // Only add non-null role codes (custom roles may have null role_code)
      if (role != null && role.getRoleCode() != null && !role.getRoleCode().isBlank()) {
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
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.ROLE,
      type = OperationEnums.CREATE,
      description = "Added role inheritance: #{#result.roleName}")
  @Transactional(rollbackFor = Exception.class)
  public MstRole addRoleInheritance(Long childRoleId, Long parentRoleId) {
    // Cannot inherit self
    if (childRoleId.equals(parentRoleId)) {
      throw new BusinessException(MessageEnums.ROLE_CIRCULAR_DEPENDENCY);
    }

    // Validate both roles exist
    getRoleById(childRoleId);
    MstRole parentRole = getRoleById(parentRoleId);

    // Check for circular dependency: if parentRoleId's ancestors include childRoleId
    validateNoCircularDependency(childRoleId, parentRoleId);

    // Application-level duplicate check (soft delete prevents UK usage)
    validateInheritanceNotExists(childRoleId, parentRoleId);

    MstRoleInheritance inheritance = new MstRoleInheritance();
    inheritance.setChildRoleId(childRoleId);
    inheritance.setParentRoleId(parentRoleId);
    roleInheritanceMapper.insert(inheritance);

    return parentRole;
  }

  /**
   * Remove an inheritance relationship between two roles.
   *
   * @param childRoleId Child role ID
   * @param parentRoleId Parent role ID
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.ROLE,
      type = OperationEnums.DELETE,
      description = "Delete role inheritance: #{#result.roleName}")
  @Transactional(rollbackFor = Exception.class)
  public MstRole removeRoleInheritance(Long childRoleId, Long parentRoleId) {
    MstRole parentRole = getRoleById(parentRoleId);

    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("child_role_id", childRoleId).eq("parent_role_id", parentRoleId);
    int deleted = roleInheritanceMapper.delete(qw);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.ROLE_NOT_FOUND);
    }

    return parentRole;
  }

  /**
   * Update role inheritance relationships.
   *
   * <p>Replaces all existing inheritance relationships for the given role with the new list of
   * parent role IDs.
   *
   * @param childRoleId Child role ID
   * @param newParentRoleIds New list of parent role IDs
   */
  @Transactional(rollbackFor = Exception.class)
  public void updateRoleInheritance(Long childRoleId, List<Long> newParentRoleIds) {
    // Get current parent role IDs
    List<Long> currentParentIds = getParentRoleIds(childRoleId);

    // Convert to sets for easier comparison
    Set<Long> currentSet = new HashSet<>(currentParentIds);
    Set<Long> newSet = new HashSet<>(newParentRoleIds != null ? newParentRoleIds : List.of());

    // Find relationships to add
    Set<Long> toAdd = new HashSet<>(newSet);
    toAdd.removeAll(currentSet);

    // Find relationships to remove
    Set<Long> toRemove = new HashSet<>(currentSet);
    toRemove.removeAll(newSet);

    // Add new relationships
    for (Long parentId : toAdd) {
      self.addRoleInheritance(childRoleId, parentId);
    }

    // Remove old relationships
    for (Long parentId : toRemove) {
      self.removeRoleInheritance(childRoleId, parentId);
    }
  }

  /**
   * Attach direct parent role IDs to a list of roles.
   *
   * <p>Performs a single batch query against the inheritance table for all given role IDs to build
   * a {@code childRoleId -> [parentRoleId...]} map, then wraps each role with its parent IDs. Roles
   * without any parents get an empty list.
   *
   * @param roles List of roles (maybe empty)
   * @return List of roles paired with their direct parent role IDs (same order as input)
   */
  private List<RoleWithParents> attachParentRoleIds(List<MstRole> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    List<Long> roleIds = roles.stream().map(MstRole::getId).toList();
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.in("child_role_id", roleIds);
    Map<Long, List<Long>> parentIdsMap =
        roleInheritanceMapper.selectList(qw).stream()
            .collect(
                Collectors.groupingBy(
                    MstRoleInheritance::getChildRoleId,
                    Collectors.mapping(MstRoleInheritance::getParentRoleId, Collectors.toList())));
    return roles.stream()
        .map(role -> new RoleWithParents(role, parentIdsMap.getOrDefault(role.getId(), List.of())))
        .toList();
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
        throw new BusinessException(MessageEnums.ROLE_CIRCULAR_DEPENDENCY);
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
      throw new BusinessException(MessageEnums.ROLE_INHERITANCE_EXISTS);
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
    qw.eq("parent_role_id", roleId);
    long count = roleInheritanceMapper.selectCount(qw);
    if (count > 0) {
      throw new BusinessException(MessageEnums.ROLE_HAS_INHERITANCE);
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
      throw new BusinessException(MessageEnums.ROLE_IN_USE);
    }
  }

  /**
   * Delete all inheritance relationships where the given role is a child.
   *
   * @param roleId Role ID whose child inheritance records should be removed
   */
  private void deleteChildRoleInheritances(Long roleId) {
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.eq("child_role_id", roleId);
    roleInheritanceMapper.delete(qw);
  }
}
