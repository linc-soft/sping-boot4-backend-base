package com.lincsoft.controller.master;

import com.lincsoft.controller.master.vo.PositionCreateRequest;
import com.lincsoft.controller.master.vo.PositionDeleteRequest;
import com.lincsoft.controller.master.vo.PositionInfoResponse;
import com.lincsoft.controller.master.vo.PositionListRequest;
import com.lincsoft.controller.master.vo.PositionUpdateRequest;
import com.lincsoft.mapstruct.PositionMapper;
import com.lincsoft.services.master.PositionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Position controller.
 *
 * <p>Provides endpoints for position CRUD operations.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
public class PositionController {

  /** Position service. */
  private final PositionService positionService;

  /** Position mapper for converting between VO and entity. */
  private final PositionMapper positionMapper;

  /**
   * Get position by ID.
   *
   * @param id Position ID
   * @return Position info response
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).POSITION_READ.roleCode)")
  public PositionInfoResponse getPosition(@PathVariable Long id) {
    return positionMapper.toInfoResponse(positionService.getPositionById(id));
  }

  /**
   * Get position list by query conditions.
   *
   * @param request Query parameters (positionName, status)
   * @return List of position items
   */
  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).POSITION_READ.roleCode)")
  public List<PositionInfoResponse> getPositionList(PositionListRequest request) {
    return positionMapper.toInfoResponseList(
        positionService.getPositionList(request.positionName(), request.status()));
  }

  /**
   * Create a new position.
   *
   * @param request Position create request
   * @return created position ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).POSITION_WRITE.roleCode)")
  public Long createPosition(@Valid @RequestBody PositionCreateRequest request) {
    return positionService.createPosition(positionMapper.toEntity(request));
  }

  /**
   * Update an existing position.
   *
   * @param request Position update request
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).POSITION_WRITE.roleCode)")
  public void updatePosition(@Valid @RequestBody PositionUpdateRequest request) {
    positionService.updatePosition(positionMapper.toEntity(request));
  }

  /**
   * Delete a position.
   *
   * @param request Position delete request
   */
  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).POSITION_DELETE.roleCode)")
  public void deletePosition(@Valid @RequestBody PositionDeleteRequest request) {
    positionService.deletePosition(
        positionService.getPositionById(request.id()), request.version());
  }
}
