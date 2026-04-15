package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.DataScopeType;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.entity.master.MstRoleDataScope;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstRoleDataScopeMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Role data scope service.
 *
 * <p>Manages department-based data scope configurations for roles. Each configuration defines which
 * departments a role can access and with what permissions.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Service
@RequiredArgsConstructor
public class RoleDataScopeService {

  private final MstRoleDataScopeMapper roleDataScopeMapper;

  /**
   * Get all data scope configurations for a given role.
   *
   * @param roleId role ID
   * @return list of data scope configurations
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Data Scope",
      type = OperationType.QUERY,
      description = "Query data scopes for role #{roleId}, return #{result.size()} records")
  public List<MstRoleDataScope> getRoleDataScopeList(Long roleId) {
    return roleDataScopeMapper.selectList(
        new QueryWrapper<MstRoleDataScope>().eq("role_id", roleId));
  }

  /**
   * Create a new role data scope configuration.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>{@code scopeType} must be a valid {@link DataScopeType} value
   *   <li>{@code deptId} must be non-null when {@code scopeType} is DEPT or DEPT_AND_CHILD
   *   <li>{@code permBits} must be in the range [1, 15]
   * </ul>
   *
   * @param scope the data scope configuration to create
   * @return the generated record ID
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Data Scope",
      type = OperationType.CREATE,
      description = "Role data scope created for role #{scope.roleId}")
  public Long createRoleDataScope(MstRoleDataScope scope) {
    validateScope(scope);
    roleDataScopeMapper.insert(scope);
    return scope.getId();
  }

  /**
   * Update an existing role data scope configuration.
   *
   * @param scope the data scope configuration with updated values
   * @throws BusinessException if validation fails or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Data Scope",
      type = OperationType.UPDATE,
      description = "Role data scope updated: #{scope.id}")
  public void updateRoleDataScope(MstRoleDataScope scope) {
    validateScope(scope);
    int updated = roleDataScopeMapper.updateById(scope);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "role data scope");
    }
  }

  /**
   * Delete a role data scope configuration.
   *
   * @param id record ID
   * @param version version for optimistic locking
   * @throws BusinessException if the record is not found or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Role Data Scope",
      type = OperationType.DELETE,
      description = "Role data scope deleted: #{id}")
  public void deleteRoleDataScope(Long id, Integer version) {
    MstRoleDataScope scope = roleDataScopeMapper.selectById(id);
    if (scope == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "role data scope");
    }
    scope.setVersion(version);
    int deleted = roleDataScopeMapper.deleteById(scope);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "role data scope");
    }
  }

  /**
   * Validate a role data scope configuration before insert or update.
   *
   * @param scope the configuration to validate
   * @throws BusinessException if any validation rule is violated
   */
  private void validateScope(MstRoleDataScope scope) {
    // Validate scopeType is a known enum value
    try {
      DataScopeType.valueOf(scope.getScopeType());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(MessageEnums.BAD_REQUEST);
    }

    // DEPT and DEPT_AND_CHILD require a non-null deptId
    DataScopeType type = DataScopeType.valueOf(scope.getScopeType());
    if ((type == DataScopeType.DEPT || type == DataScopeType.DEPT_AND_CHILD)
        && scope.getDeptId() == null) {
      throw new BusinessException(MessageEnums.BAD_REQUEST);
    }

    // permBits must be in [1, 15]
    if (scope.getPermBits() == null || scope.getPermBits() < 1 || scope.getPermBits() > 15) {
      throw new BusinessException(MessageEnums.INVALID_PERM_BITS);
    }
  }
}
