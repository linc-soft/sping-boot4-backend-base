package com.lincsoft.mapstruct;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.AccessLogDetailResponse;
import com.lincsoft.controller.log.vo.AccessLogPageResponseItem;
import com.lincsoft.entity.system.SysAccessLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Access Log mapper for converting between VO and SysAccessLog.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Mapper(componentModel = "spring")
public interface AccessLogMapper {
  /**
   * Convert SysAccessLog to AccessLogPageResponseItem.
   *
   * @param entity SysAccessLog entity
   * @return AccessLogPageResponseItem VO
   */
  @Mapping(target = "method", source = "requestMethod")
  @Mapping(target = "path", source = "requestUrl")
  @Mapping(
      target = "statusCode",
      expression = "java(entity.getResponseStatus() != null ? entity.getResponseStatus() : 0)")
  @Mapping(target = "createdAt", source = "createTime")
  AccessLogPageResponseItem toPageResponseItem(SysAccessLog entity);

  /**
   * Convert IPage of SysAccessLog to IPage of AccessLogPageResponseItem.
   *
   * @param entities IPage of SysAccessLog entities
   * @return IPage of AccessLogPageResponseItem VO
   */
  default IPage<AccessLogPageResponseItem> toPageResponse(IPage<SysAccessLog> entities) {
    return entities.convert(this::toPageResponseItem);
  }

  /**
   * Convert SysAccessLog to AccessLogDetailResponse.
   *
   * @param entity SysAccessLog entity
   * @return AccessLogDetailResponse VO
   */
  @Mapping(target = "method", source = "requestMethod")
  @Mapping(target = "path", source = "requestUrl")
  @Mapping(target = "queryString", source = "requestParams")
  @Mapping(
      target = "statusCode",
      expression = "java(entity.getResponseStatus() != null ? entity.getResponseStatus() : 0)")
  @Mapping(target = "createdAt", source = "createTime")
  AccessLogDetailResponse toDetailResponse(SysAccessLog entity);
}
