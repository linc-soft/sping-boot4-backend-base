package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.entity.master.MstDept;
import com.lincsoft.entity.master.MstUserDept;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstDeptMapper;
import com.lincsoft.mapper.master.MstUserDeptMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Department service.
 *
 * <p>Manages the department tree structure used as the base dimension for data permission control.
 * Supports unlimited depth via the self-referencing {@code parentId} field.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Service
@RequiredArgsConstructor
public class DeptService {

  private final MstDeptMapper deptMapper;
  private final MstUserDeptMapper userDeptMapper;

  /** Self reference for lazy initialization (avoids circular proxy issues). */
  @Lazy private final DeptService self;

  /**
   * Get the full department list (flat, ordered by sort_order).
   *
   * @return all non-deleted departments
   */
  @OperationLog(
      module = "Master",
      subModule = "Dept Manager",
      type = OperationType.QUERY,
      description = "Query dept tree, return #{result.size()} depts")
  public List<MstDept> getDeptTree() {
    return deptMapper.selectList(new QueryWrapper<MstDept>().orderByAsc("parent_id", "sort_order"));
  }

  /**
   * Get a department by ID.
   *
   * @param id department ID
   * @return the department entity
   * @throws BusinessException if the department does not exist
   */
  public MstDept getDeptById(Long id) {
    MstDept dept = deptMapper.selectById(id);
    if (dept == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "dept");
    }
    return dept;
  }

  /**
   * Create a new department.
   *
   * <p>Validates that the parent department exists (if specified) and that the operation does not
   * create a circular reference.
   *
   * @param dept the department to create
   * @return the generated department ID
   */
  @OperationLog(
      module = "Master",
      subModule = "Dept Manager",
      type = OperationType.CREATE,
      description = "Dept created: #{dept.deptName}")
  public Long createDept(MstDept dept) {
    // Validate parent exists
    if (dept.getParentId() != null) {
      self.getDeptById(dept.getParentId());
    }
    deptMapper.insert(dept);
    return dept.getId();
  }

  /**
   * Update an existing department.
   *
   * <p>Validates that the parent department exists (if specified) and that the update does not
   * create a circular reference (e.g. setting a descendant as the new parent).
   *
   * @param dept the department with updated values
   * @throws BusinessException if parent not found, circular reference detected, or optimistic lock
   *     fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Dept Manager",
      type = OperationType.UPDATE,
      description = "Dept updated: #{dept.deptName}")
  public void updateDept(MstDept dept) {
    // Validate parent exists
    if (dept.getParentId() != null) {
      self.getDeptById(dept.getParentId());
      // Detect circular reference: new parent must not be a descendant of this dept
      Set<Long> descendants = self.collectDescendantIds(dept.getId());
      if (descendants.contains(dept.getParentId())) {
        throw new BusinessException(MessageEnums.DEPT_CIRCULAR_REFERENCE);
      }
    }
    int updated = deptMapper.updateById(dept);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "dept");
    }
  }

  /**
   * Delete a department.
   *
   * <p>Refuses deletion if the department has any active child departments.
   *
   * @param id department ID
   * @param version version for optimistic locking
   * @throws BusinessException if the department has children, is not found, or optimistic lock
   *     fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Dept Manager",
      type = OperationType.DELETE,
      description = "Dept deleted: #{dept.deptName}")
  public void deleteDept(Long id, Integer version) {
    MstDept dept = self.getDeptById(id);

    // Refuse deletion if child departments exist
    long childCount = deptMapper.selectCount(new QueryWrapper<MstDept>().eq("parent_id", id));
    if (childCount > 0) {
      throw new BusinessException(MessageEnums.DEPT_HAS_CHILDREN);
    }

    dept.setVersion(version);
    int deleted = deptMapper.deleteById(dept);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "dept");
    }
  }

  /**
   * Recursively collect all descendant department IDs, including the given department itself.
   *
   * <p>This method performs a breadth-first traversal of the department tree starting from the
   * given node. The result is used by the data permission system to resolve {@code DEPT_AND_CHILD}
   * scope types.
   *
   * @param deptId the root department ID to start from
   * @return a set containing {@code deptId} and all its descendant IDs
   */
  public Set<Long> collectDescendantIds(Long deptId) {
    Set<Long> result = new HashSet<>();
    collectDescendantsRecursive(deptId, result);
    return result;
  }

  /**
   * Internal recursive helper for {@link #collectDescendantIds(Long)}.
   *
   * @param deptId current node ID
   * @param result accumulator set
   */
  private void collectDescendantsRecursive(Long deptId, Set<Long> result) {
    // Guard against cycles (should not occur in valid data, but defensive check)
    if (!result.add(deptId)) {
      return;
    }
    List<MstDept> children =
        deptMapper.selectList(new QueryWrapper<MstDept>().eq("parent_id", deptId));
    for (MstDept child : children) {
      collectDescendantsRecursive(child.getId(), result);
    }
  }

  /**
   * Get all department IDs that the given user belongs to.
   *
   * @param userId user ID
   * @return set of department IDs
   */
  public Set<Long> getUserDeptIds(Long userId) {
    List<MstUserDept> userDepts =
        userDeptMapper.selectList(new QueryWrapper<MstUserDept>().eq("user_id", userId));
    Set<Long> deptIds = new HashSet<>();
    for (MstUserDept ud : userDepts) {
      deptIds.add(ud.getDeptId());
    }
    return deptIds;
  }

  /**
   * Collect all ancestor department IDs for the given department (excluding itself).
   *
   * <p>Traverses the parent chain upward until reaching a root node (parentId == null).
   *
   * @param deptId starting department ID
   * @return set of ancestor department IDs
   */
  public Set<Long> getAncestorDeptIds(Long deptId) {
    Set<Long> ancestors = new HashSet<>();
    MstDept current = deptMapper.selectById(deptId);
    while (current != null && current.getParentId() != null) {
      Long parentId = current.getParentId();
      // Guard against cycles
      if (ancestors.contains(parentId)) {
        break;
      }
      ancestors.add(parentId);
      current = deptMapper.selectById(parentId);
    }
    return ancestors;
  }
}
