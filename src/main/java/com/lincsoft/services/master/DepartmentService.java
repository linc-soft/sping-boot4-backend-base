package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.Module;
import com.lincsoft.constant.OperationType;
import com.lincsoft.constant.SubModule;
import com.lincsoft.controller.master.vo.DepartmentTreeResponse;
import com.lincsoft.entity.master.MstDepartment;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstDepartmentMapper;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Department service.
 *
 * <p>Provides business logic for department management, including tree assembly, cyclic-parent
 * prevention, code uniqueness, and delete guards.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {
  /** Top-level departments use this sentinel as their parent ID. */
  private static final long ROOT_PARENT_ID = 0L;

  /** Department mapper for database operations. */
  private final MstDepartmentMapper departmentMapper;

  private final UserService userService;

  /**
   * Get department by ID.
   *
   * @param id Department ID
   * @return MstDepartment entity
   * @throws BusinessException if the department is not found
   */
  public MstDepartment getDepartmentById(Long id) {
    MstDepartment department = departmentMapper.selectById(id);
    if (department == null) {
      throw new BusinessException(MessageEnums.DEPT_NOT_FOUND);
    }
    return department;
  }

  /**
   * Get the full department tree.
   *
   * <p>Loads all departments, then assembles them into a nested tree. Siblings are ordered by
   * {@code sortOrder} ascending, then by {@code id} ascending. Orphaned nodes (whose parent is
   * missing or soft-deleted) are surfaced as top-level nodes so they remain visible.
   *
   * @return List of top-level department tree nodes
   */
  public List<DepartmentTreeResponse> getDepartmentTree() {
    List<MstDepartment> all = departmentMapper.selectList(new QueryWrapper<>());
    Set<Long> presentIds = all.stream().map(MstDepartment::getId).collect(Collectors.toSet());

    Map<Long, List<MstDepartment>> childrenByParent =
        all.stream().collect(Collectors.groupingBy(d -> resolveParentKey(d, presentIds)));

    return buildNodes(childrenByParent.get(ROOT_PARENT_ID), childrenByParent);
  }

  /**
   * Create a new department.
   *
   * <p>Validates parent existence and department code uniqueness before inserting. A blank or null
   * parent ID is normalized to the top-level sentinel.
   *
   * @param department MstDepartment entity to create
   * @return The created department ID
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.DEPARTMENT,
      type = OperationType.CREATE,
      description = "Department created: #{#department.deptName}")
  @Transactional(rollbackFor = Exception.class)
  public Long createDepartment(MstDepartment department) {
    normalizeParentId(department);
    validateParentExists(department.getParentId());
    validateCodeUnique(department.getDeptCode(), null);

    departmentMapper.insert(department);
    return department.getId();
  }

  /**
   * Update an existing department.
   *
   * <p>Validates parent existence, code uniqueness (excluding itself), and that the new parent does
   * not create a cycle. Uses optimistic locking via the version field.
   *
   * @param department MstDepartment entity with updated values
   * @throws BusinessException if validation fails or optimistic lock fails
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.DEPARTMENT,
      type = OperationType.UPDATE,
      description = "Department updated: #{#department.deptName}")
  @Transactional(rollbackFor = Exception.class)
  public void updateDepartment(MstDepartment department) {
    normalizeParentId(department);
    getDepartmentById(department.getId());
    validateParentExists(department.getParentId());
    validateCodeUnique(department.getDeptCode(), department.getId());
    validateNoCycle(department.getId(), department.getParentId());

    int updated = departmentMapper.updateById(department);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.DEPT_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Delete a department.
   *
   * <p>Refuses deletion when the department has any sub-department or assigned user. Uses
   * optimistic locking via an explicit version condition (logical delete does not apply the
   * {@code @Version} check).
   *
   * @param department MstDepartment entity to delete
   * @param version Version for optimistic locking
   * @throws BusinessException if the department has children, has users, or optimistic lock fails
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.DEPARTMENT,
      type = OperationType.DELETE,
      description = "Department deleted: #{#department.deptName}")
  @Transactional(rollbackFor = Exception.class)
  public void deleteDepartment(MstDepartment department, Integer version) {
    validateNoChildren(department.getId());
    validateNoUsersInDept(department.getId());

    LambdaUpdateWrapper<MstDepartment> deleteWrapper = new LambdaUpdateWrapper<>();
    deleteWrapper
        .eq(MstDepartment::getId, department.getId())
        .eq(MstDepartment::getVersion, version);
    int deleted = departmentMapper.delete(deleteWrapper);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.DEPT_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Resolve the grouping key for a department's parent.
   *
   * <p>Departments whose parent is the root sentinel, or whose parent is absent from the present
   * set (missing or soft-deleted), are grouped under the root so they stay visible.
   *
   * @param department Department to resolve
   * @param presentIds IDs of all present (non-deleted) departments
   * @return The parent ID to group under, or the root sentinel
   */
  private Long resolveParentKey(MstDepartment department, Set<Long> presentIds) {
    Long parentId = department.getParentId();
    if (parentId == null || parentId == ROOT_PARENT_ID || !presentIds.contains(parentId)) {
      return ROOT_PARENT_ID;
    }
    return parentId;
  }

  /**
   * Recursively build tree nodes for a list of sibling departments.
   *
   * @param siblings Sibling departments at the current level (may be null)
   * @param childrenByParent Map of parent ID to its direct children
   * @return Ordered list of tree nodes with their descendants attached
   */
  private List<DepartmentTreeResponse> buildNodes(
      List<MstDepartment> siblings, Map<Long, List<MstDepartment>> childrenByParent) {
    if (siblings == null || siblings.isEmpty()) {
      return List.of();
    }
    return siblings.stream()
        .sorted(
            Comparator.comparing(
                    MstDepartment::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MstDepartment::getId))
        .map(
            d ->
                new DepartmentTreeResponse(
                    d.getId(),
                    d.getDeptName(),
                    d.getDeptCode(),
                    d.getParentId(),
                    d.getLeaderUserId(),
                    d.getSortOrder(),
                    d.getStatus(),
                    d.getVersion(),
                    buildNodes(childrenByParent.get(d.getId()), childrenByParent)))
        .toList();
  }

  /**
   * Normalize a null parent ID to the top-level sentinel.
   *
   * @param department Department to normalize
   */
  private void normalizeParentId(MstDepartment department) {
    if (department.getParentId() == null) {
      department.setParentId(ROOT_PARENT_ID);
    }
  }

  /**
   * Validate that the parent department exists (unless top-level).
   *
   * @param parentId Parent department ID
   * @throws BusinessException if a non-root parent does not exist
   */
  private void validateParentExists(Long parentId) {
    if (parentId == null || parentId == ROOT_PARENT_ID) {
      return;
    }
    if (departmentMapper.selectById(parentId) == null) {
      throw new BusinessException(MessageEnums.DEPT_PARENT_NOT_FOUND);
    }
  }

  /**
   * Validate that the department code is unique among non-deleted departments.
   *
   * @param deptCode Department code to check (skipped when null or blank)
   * @param excludeId Department ID to exclude from the check (null when creating)
   * @throws BusinessException if the code already exists
   */
  private void validateCodeUnique(String deptCode, Long excludeId) {
    if (deptCode == null || deptCode.isBlank()) {
      return;
    }
    QueryWrapper<MstDepartment> qw = new QueryWrapper<>();
    qw.eq("dept_code", deptCode);
    if (excludeId != null) {
      qw.ne("id", excludeId);
    }
    if (departmentMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.DEPT_CODE_EXISTS);
    }
  }

  /**
   * Validate that reparenting a department does not create a cycle.
   *
   * <p>A department cannot become a child of itself or of any of its own descendants. Walks upward
   * from the proposed parent; if the department itself is reached, a cycle would form.
   *
   * @param deptId Department being reparented
   * @param newParentId Proposed new parent ID
   * @throws BusinessException if a cycle would be created
   */
  private void validateNoCycle(Long deptId, Long newParentId) {
    if (newParentId == null || newParentId == ROOT_PARENT_ID) {
      return;
    }
    if (deptId.equals(newParentId)) {
      throw new BusinessException(MessageEnums.DEPT_CIRCULAR_DEPENDENCY);
    }
    Set<Long> visited = new HashSet<>();
    Queue<Long> queue = new LinkedList<>();
    queue.add(newParentId);
    while (!queue.isEmpty()) {
      Long current = queue.poll();
      if (current.equals(deptId)) {
        throw new BusinessException(MessageEnums.DEPT_CIRCULAR_DEPENDENCY);
      }
      if (!visited.add(current)) {
        continue;
      }
      MstDepartment parent = departmentMapper.selectById(current);
      if (parent != null
          && parent.getParentId() != null
          && parent.getParentId() != ROOT_PARENT_ID) {
        queue.add(parent.getParentId());
      }
    }
  }

  /**
   * Validate that the department has no sub-departments.
   *
   * @param deptId Department ID to check
   * @throws BusinessException if the department has any child
   */
  private void validateNoChildren(Long deptId) {
    QueryWrapper<MstDepartment> qw = new QueryWrapper<>();
    qw.eq("parent_id", deptId);
    if (departmentMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.DEPT_HAS_CHILDREN);
    }
  }

  /**
   * Validate that the department has no users assigned.
   *
   * @param deptId Department ID to check
   * @throws BusinessException if any user belongs to the department
   */
  private void validateNoUsersInDept(Long deptId) {
    if (userService.countByDeptId(deptId) > 0) {
      throw new BusinessException(MessageEnums.DEPT_HAS_USERS);
    }
  }
}
