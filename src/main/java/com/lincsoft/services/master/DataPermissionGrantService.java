package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.constant.ResourceType;
import com.lincsoft.constant.SubjectType;
import com.lincsoft.entity.master.MstDataPermissionGrant;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstDataPermissionGrantMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Row-level data permission grant service.
 *
 * <p>Manages fine-grained permission grants that allow specific subjects (users, roles, or
 * departments) to access specific resource instances with defined permission bits.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Service
@RequiredArgsConstructor
public class DataPermissionGrantService {

  private final MstDataPermissionGrantMapper grantMapper;

  /**
   * Get all grants for a specific resource.
   *
   * @param resourceType resource type enum name
   * @param resourceId resource instance ID
   * @return list of grants
   */
  @OperationLog(
      module = "Master",
      subModule = "Data Permission Grant",
      type = OperationType.QUERY,
      description =
          "Query grants for #{resourceType}:#{resourceId}, return #{result.size()} records")
  public List<MstDataPermissionGrant> getGrantList(String resourceType, Long resourceId) {
    return grantMapper.selectList(
        new QueryWrapper<MstDataPermissionGrant>()
            .eq("resource_type", resourceType)
            .eq("resource_id", resourceId));
  }

  /**
   * Create a new row-level permission grant.
   *
   * <p>Validates:
   *
   * <ul>
   *   <li>{@code subjectType} must be a valid {@link SubjectType} value
   *   <li>{@code resourceType} must be a valid {@link ResourceType} value
   *   <li>{@code permBits} must be in the range [1, 15]
   *   <li>If both {@code validFrom} and {@code validUntil} are non-null, {@code validFrom} must be
   *       before {@code validUntil}
   * </ul>
   *
   * @param grant the grant to create
   * @return the generated record ID
   */
  @OperationLog(
      module = "Master",
      subModule = "Data Permission Grant",
      type = OperationType.CREATE,
      description =
          "Grant created: #{grant.subjectType}:#{grant.subjectId} ->"
              + " #{grant.resourceType}:#{grant.resourceId}")
  public Long createGrant(MstDataPermissionGrant grant) {
    validateGrant(grant);
    grantMapper.insert(grant);
    return grant.getId();
  }

  /**
   * Update an existing permission grant.
   *
   * @param grant the grant with updated values
   * @throws BusinessException if validation fails or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Data Permission Grant",
      type = OperationType.UPDATE,
      description = "Grant updated: #{grant.id}")
  public void updateGrant(MstDataPermissionGrant grant) {
    validateGrant(grant);
    int updated = grantMapper.updateById(grant);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "data permission grant");
    }
  }

  /**
   * Delete a permission grant.
   *
   * @param id grant ID
   * @param version version for optimistic locking
   * @throws BusinessException if the grant is not found or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "Data Permission Grant",
      type = OperationType.DELETE,
      description = "Grant deleted: #{id}")
  public void deleteGrant(Long id, Integer version) {
    MstDataPermissionGrant grant = grantMapper.selectById(id);
    if (grant == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "data permission grant");
    }
    grant.setVersion(version);
    int deleted = grantMapper.deleteById(grant);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "data permission grant");
    }
  }

  /**
   * Validate a grant before insert or update.
   *
   * @param grant the grant to validate
   * @throws BusinessException if any validation rule is violated
   */
  private void validateGrant(MstDataPermissionGrant grant) {
    // Validate subjectType
    try {
      SubjectType.valueOf(grant.getSubjectType());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(MessageEnums.BAD_REQUEST);
    }

    // Validate resourceType
    try {
      ResourceType.valueOf(grant.getResourceType());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(MessageEnums.BAD_REQUEST);
    }

    // permBits must be in [1, 15]
    if (grant.getPermBits() == null || grant.getPermBits() < 1 || grant.getPermBits() > 15) {
      throw new BusinessException(MessageEnums.INVALID_PERM_BITS);
    }

    // validFrom must be before validUntil when both are present
    if (grant.getValidFrom() != null
        && grant.getValidUntil() != null
        && !grant.getValidFrom().isBefore(grant.getValidUntil())) {
      throw new BusinessException(MessageEnums.INVALID_VALID_PERIOD);
    }
  }
}
