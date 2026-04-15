package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.*;
import com.lincsoft.entity.master.MstUser;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * User mapper for converting between VO and MstUser.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
  /**
   * Convert UserCreateRequest to MstUser.
   *
   * <p>Fields managed by framework (id, audit fields, deleted, version) are intentionally ignored.
   *
   * @param request User create request
   * @return MstUser entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstUser toEntity(UserCreateRequest request);

  /**
   * Convert UserUpdateRequest to MstUser.
   *
   * <p>Framework-managed audit fields (createBy, createAt, updateBy, updateAt, deleted) are
   * intentionally ignored. The id and version fields are mapped from the request for update
   * operations.
   *
   * @param request User update request
   * @return MstUser entity
   */
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  MstUser toEntity(UserUpdateRequest request);

  /**
   * Convert MstUser to UserInfoResponse.
   *
   * @param entity MstUser entity
   * @return UserInfoResponse VO
   */
  UserInfoResponse toInfoResponse(MstUser entity);

  /**
   * Convert MstUser to UserListResponseItem.
   *
   * @param entity MstUser entity
   * @return UserListResponseItem VO
   */
  UserListResponseItem toListResponseItem(MstUser entity);

  /**
   * Convert list of MstUser to list of UserListResponseItem.
   *
   * @param entities List of MstUser entities
   * @return List of UserListResponseItem VO
   */
  List<UserListResponseItem> toListResponse(List<MstUser> entities);
}
