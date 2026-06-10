package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.PositionCreateRequest;
import com.lincsoft.controller.master.vo.PositionInfoResponse;
import com.lincsoft.controller.master.vo.PositionUpdateRequest;
import com.lincsoft.entity.master.MstPosition;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Position mapper for converting between position VOs and MstPosition.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Mapper(componentModel = "spring")
public interface PositionMapper {
  /**
   * Convert PositionCreateRequest to MstPosition.
   *
   * <p>Framework-managed fields (id, audit fields, deleted, version) are intentionally ignored.
   *
   * @param request Position create request
   * @return MstPosition entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstPosition toEntity(PositionCreateRequest request);

  /**
   * Convert PositionUpdateRequest to MstPosition.
   *
   * <p>Framework-managed audit fields are intentionally ignored. The id and version fields are
   * mapped from the request for update operations.
   *
   * @param request Position update request
   * @return MstPosition entity
   */
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  MstPosition toEntity(PositionUpdateRequest request);

  /**
   * Convert MstPosition to PositionInfoResponse.
   *
   * @param entity MstPosition entity
   * @return PositionInfoResponse VO
   */
  PositionInfoResponse toInfoResponse(MstPosition entity);

  /**
   * Convert a list of MstPosition to a list of PositionInfoResponse.
   *
   * @param entities List of MstPosition entities
   * @return List of PositionInfoResponse VO
   */
  List<PositionInfoResponse> toInfoResponseList(List<MstPosition> entities);
}
