package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.DepartmentCreateRequest;
import com.lincsoft.controller.master.vo.DepartmentInfoResponse;
import com.lincsoft.controller.master.vo.DepartmentUpdateRequest;
import com.lincsoft.entity.master.MstDepartment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Department mapper for converting between department VOs and MstDepartment.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Mapper(componentModel = "spring")
public interface DepartmentMapper {
  /**
   * Convert DepartmentCreateRequest to MstDepartment.
   *
   * <p>Framework-managed fields (id, audit fields, deleted, version) are intentionally ignored.
   *
   * @param request Department create request
   * @return MstDepartment entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstDepartment toEntity(DepartmentCreateRequest request);

  /**
   * Convert DepartmentUpdateRequest to MstDepartment.
   *
   * <p>Framework-managed audit fields are intentionally ignored. The id and version fields are
   * mapped from the request for update operations.
   *
   * @param request Department update request
   * @return MstDepartment entity
   */
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  MstDepartment toEntity(DepartmentUpdateRequest request);

  /**
   * Convert MstDepartment to DepartmentInfoResponse.
   *
   * @param entity MstDepartment entity
   * @return DepartmentInfoResponse VO
   */
  DepartmentInfoResponse toInfoResponse(MstDepartment entity);
}
