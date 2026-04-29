package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.*;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.services.master.RoleWithParents;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Role mapper for converting between RoleCreateRequest and MstRole.
 *
 * @author 林创科技
 * @since 2026-04-14
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {
  /**
   * Convert RoleCreateRequest to MstRole.
   *
   * <p>Fields managed by framework (id, audit fields, deleted, version) are intentionally ignored.
   *
   * @param request Role create request
   * @return MstRole entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstRole toEntity(RoleCreateRequest request);

  /**
   * Convert RoleUpdateRequest to MstRole.
   *
   * <p>Framework-managed audit fields (createBy, createAt, updateBy, updateAt, deleted) are
   * intentionally ignored. The id and version fields are mapped from the request for update
   * operations.
   *
   * @param request Role update request
   * @return MstRole entity
   */
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  MstRole toEntity(RoleUpdateRequest request);

  /**
   * Convert MstRole to RoleInfoResponse.
   *
   * @param entity MstRole entity
   * @return RoleInfoResponse VO
   */
  RoleInfoResponse toInfoResponse(MstRole entity);

  /**
   * Convert a role bundled with its direct parent role IDs to a RoleListResponseItem.
   *
   * @param dto Role together with its direct parent role IDs
   * @return RoleListResponseItem VO
   */
  @Mapping(target = "id", source = "role.id")
  @Mapping(target = "roleName", source = "role.roleName")
  @Mapping(target = "roleCode", source = "role.roleCode")
  @Mapping(target = "description", source = "role.description")
  @Mapping(target = "updateBy", source = "role.updateBy")
  @Mapping(target = "updateAt", source = "role.updateAt")
  @Mapping(target = "parentRoleIds", source = "parentRoleIds")
  RoleListResponseItem toListResponseItem(RoleWithParents dto);

  /**
   * Convert list of roles-with-parents to list of RoleListResponseItem.
   *
   * @param list List of roles paired with their direct parent role IDs
   * @return List of RoleListResponseItem VO
   */
  List<RoleListResponseItem> toListResponse(List<RoleWithParents> list);
}
