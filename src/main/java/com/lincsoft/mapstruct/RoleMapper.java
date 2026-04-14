package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.RoleCreateRequest;
import com.lincsoft.entity.master.MstRole;
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
}
