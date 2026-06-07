package com.lincsoft.mapstruct;

import com.lincsoft.controller.oa.vo.EmployeeCreateRequest;
import com.lincsoft.controller.oa.vo.EmployeeInfoResponse;
import com.lincsoft.controller.oa.vo.EmployeePageResponseItem;
import com.lincsoft.controller.oa.vo.EmployeeUpdateRequest;
import com.lincsoft.entity.oa.MstEmployee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Employee mapper for converting between employee VOs and MstEmployee.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Mapper(componentModel = "spring")
public interface EmployeeMapper {
  /**
   * Convert EmployeeCreateRequest to MstEmployee.
   *
   * <p>{@code userId} is intentionally ignored: it is assigned by the service after the linked
   * login account is created. Framework-managed fields are also ignored. The {@code username} and
   * {@code roleIds} request fields belong to the login account and have no entity counterpart.
   *
   * @param request Employee create request
   * @return MstEmployee entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstEmployee toEntity(EmployeeCreateRequest request);

  /**
   * Convert EmployeeUpdateRequest to MstEmployee.
   *
   * <p>{@code userId} is intentionally ignored: the linked login account is managed separately.
   * Framework-managed audit fields are also ignored. The id and version fields are mapped for
   * update operations.
   *
   * @param request Employee update request
   * @return MstEmployee entity
   */
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  MstEmployee toEntity(EmployeeUpdateRequest request);

  /**
   * Convert MstEmployee to EmployeeInfoResponse.
   *
   * @param entity MstEmployee entity
   * @return EmployeeInfoResponse VO
   */
  EmployeeInfoResponse toInfoResponse(MstEmployee entity);

  /**
   * Convert MstEmployee to EmployeePageResponseItem.
   *
   * @param entity MstEmployee entity
   * @return EmployeePageResponseItem VO
   */
  EmployeePageResponseItem toPageResponseItem(MstEmployee entity);
}
