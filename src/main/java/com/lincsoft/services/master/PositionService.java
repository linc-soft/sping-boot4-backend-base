package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.ModuleEnums;
import com.lincsoft.constant.OperationEnums;
import com.lincsoft.constant.SubModuleEnums;
import com.lincsoft.entity.master.MstPosition;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstPositionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Position service.
 *
 * <p>Provides business logic for position management: CRUD with code uniqueness and optimistic
 * locking.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class PositionService {
  /** Position mapper for database operations. */
  private final MstPositionMapper positionMapper;

  /** User service for checking position occupancy before deletion. */
  private final UserService userService;

  /**
   * Get position by ID.
   *
   * @param id Position ID
   * @return MstPosition entity
   * @throws BusinessException if the position is not found
   */
  public MstPosition getPositionById(Long id) {
    MstPosition position = positionMapper.selectById(id);
    if (position == null) {
      throw new BusinessException(MessageEnums.POSITION_NOT_FOUND);
    }
    return position;
  }

  /**
   * Get position list by query conditions.
   *
   * <p>Results are ordered by {@code sortOrder} ascending (nulls last), then by {@code id}
   * ascending.
   *
   * @param positionName Position name (partial match)
   * @param status Status (exact match)
   * @return List of positions
   */
  public List<MstPosition> getPositionList(String positionName, String status) {
    QueryWrapper<MstPosition> queryWrapper = new QueryWrapper<>();
    if (positionName != null && !positionName.isBlank()) {
      queryWrapper.like("position_name", positionName);
    }
    if (status != null && !status.isBlank()) {
      queryWrapper.eq("status", status);
    }
    queryWrapper.orderByAsc("sort_order").orderByAsc("id");
    return positionMapper.selectList(queryWrapper);
  }

  /**
   * Create a new position.
   *
   * <p>Validates position code uniqueness before inserting.
   *
   * @param position MstPosition entity to create
   * @return The created position ID
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.POSITION,
      type = OperationEnums.CREATE,
      description = "Position created: #{#position.positionName}")
  @Transactional(rollbackFor = Exception.class)
  public Long createPosition(MstPosition position) {
    validateCodeUnique(position.getPositionCode(), null);
    positionMapper.insert(position);
    return position.getId();
  }

  /**
   * Update an existing position.
   *
   * <p>Validates position code uniqueness (excluding itself). Uses optimistic locking via the
   * version field.
   *
   * @param position MstPosition entity with updated values
   * @throws BusinessException if the code is duplicate or optimistic lock fails
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.POSITION,
      type = OperationEnums.UPDATE,
      description = "Position updated: #{#position.positionName}")
  @Transactional(rollbackFor = Exception.class)
  public void updatePosition(MstPosition position) {
    getPositionById(position.getId());
    validateCodeUnique(position.getPositionCode(), position.getId());

    int updated = positionMapper.updateById(position);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.POSITION_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Delete a position.
   *
   * <p>Refuses deletion when the position is assigned to any user. Uses optimistic locking via an
   * explicit version condition (logical delete does not apply the {@code @Version} check).
   *
   * @param position MstPosition entity to delete
   * @param version Version for optimistic locking
   * @throws BusinessException if the position has users or optimistic lock fails
   */
  @OperationLog(
      module = ModuleEnums.MASTER,
      subModule = SubModuleEnums.POSITION,
      type = OperationEnums.DELETE,
      description = "Position deleted: #{#position.positionName}")
  @Transactional(rollbackFor = Exception.class)
  public void deletePosition(MstPosition position, Integer version) {
    validateNoEmployees(position.getId());

    LambdaUpdateWrapper<MstPosition> deleteWrapper = new LambdaUpdateWrapper<>();
    deleteWrapper.eq(MstPosition::getId, position.getId()).eq(MstPosition::getVersion, version);
    int deleted = positionMapper.delete(deleteWrapper);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.POSITION_OPTIMISTIC_LOCK_FAILED);
    }
  }

  /**
   * Validate that the position code is unique among non-deleted positions.
   *
   * @param positionCode Position code to check (skipped when null or blank)
   * @param excludeId Position ID to exclude from the check (null when creating)
   * @throws BusinessException if the code already exists
   */
  private void validateCodeUnique(String positionCode, Long excludeId) {
    if (positionCode == null || positionCode.isBlank()) {
      return;
    }
    QueryWrapper<MstPosition> qw = new QueryWrapper<>();
    qw.eq("position_code", positionCode);
    if (excludeId != null) {
      qw.ne("id", excludeId);
    }
    if (positionMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.POSITION_CODE_EXISTS);
    }
  }

  /**
   * Validate that the position is not assigned to any user.
   *
   * @param positionId Position ID to check
   * @throws BusinessException if any user holds the position
   */
  private void validateNoEmployees(Long positionId) {
    if (userService.countByPositionId(positionId) > 0) {
      throw new BusinessException(MessageEnums.POSITION_HAS_USERS);
    }
  }
}
