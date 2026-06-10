package com.lincsoft.mapstruct;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.ErrorLogDetailResponse;
import com.lincsoft.controller.log.vo.ErrorLogPageResponseItem;
import com.lincsoft.entity.system.SysErrorLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Error Log mapper for converting between VO and SysErrorLog.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Mapper(componentModel = "spring")
public interface ErrorLogMapper {
  /**
   * Convert SysErrorLog to ErrorLogPageResponseItem.
   *
   * @param entity SysErrorLog entity
   * @return ErrorLogPageResponseItem VO
   */
  @Mapping(target = "errorType", source = "exceptionClass")
  @Mapping(target = "message", source = "exceptionMessage")
  @Mapping(target = "createdAt", source = "createTime")
  ErrorLogPageResponseItem toPageResponseItem(SysErrorLog entity);

  /**
   * Convert IPage of SysErrorLog to IPage of ErrorLogPageResponseItem.
   *
   * @param entities IPage of SysErrorLog entities
   * @return IPage of ErrorLogPageResponseItem VO
   */
  default IPage<ErrorLogPageResponseItem> toPageResponse(IPage<SysErrorLog> entities) {
    return entities.convert(this::toPageResponseItem);
  }

  /**
   * Convert SysErrorLog to ErrorLogDetailResponse.
   *
   * @param entity SysErrorLog entity
   * @return ErrorLogDetailResponse VO
   */
  @Mapping(target = "errorType", source = "exceptionClass")
  @Mapping(target = "message", source = "exceptionMessage")
  @Mapping(target = "requestPath", source = "requestUrl")
  @Mapping(target = "requestBody", ignore = true)
  @Mapping(target = "createdAt", source = "createTime")
  ErrorLogDetailResponse toDetailResponse(SysErrorLog entity);
}
