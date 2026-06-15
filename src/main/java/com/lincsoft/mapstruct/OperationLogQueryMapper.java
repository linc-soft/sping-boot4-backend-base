package com.lincsoft.mapstruct;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.constant.OperationEnums;
import com.lincsoft.controller.log.vo.OperationLogDetailResponse;
import com.lincsoft.controller.log.vo.OperationLogPageResponseItem;
import com.lincsoft.entity.system.SysOperationLog;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Operation Log mapper for converting between VO and SysOperationLog.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Mapper(componentModel = "spring")
public interface OperationLogQueryMapper {
  /**
   * Convert SysOperationLog to OperationLogPageResponseItem.
   *
   * @param entity SysOperationLog entity
   * @return OperationLogPageResponseItem VO
   */
  @Mapping(target = "createdAt", source = "createTime")
  OperationLogPageResponseItem toPageResponseItem(SysOperationLog entity);

  /**
   * Convert IPage of SysOperationLog to IPage of OperationLogPageResponseItem.
   *
   * @param entities IPage of SysOperationLog entities
   * @return IPage of OperationLogPageResponseItem VO
   */
  default IPage<OperationLogPageResponseItem> toPageResponse(IPage<SysOperationLog> entities) {
    return entities.convert(this::toPageResponseItem);
  }

  /**
   * Convert SysOperationLog to OperationLogDetailResponse.
   *
   * @param entity SysOperationLog entity
   * @return OperationLogDetailResponse VO
   */
  @Mapping(target = "createdAt", source = "createTime")
  OperationLogDetailResponse toDetailResponse(SysOperationLog entity);

  /**
   * Convert list of SysOperationLog to list of OperationLogDetailResponse.
   *
   * @param entities List of SysOperationLog entities
   * @return List of OperationLogDetailResponse VO
   */
  List<OperationLogDetailResponse> toDetailResponseList(List<SysOperationLog> entities);

  /**
   * Parse operation type string to enum.
   *
   * @param value operation type string
   * @return OperationEnums enum
   */
  default OperationEnums parseOperationType(String value) {
    if (value == null) {
      return null;
    }
    try {
      return OperationEnums.valueOf(value);
    } catch (IllegalArgumentException e) {
      return OperationEnums.OTHER;
    }
  }
}
