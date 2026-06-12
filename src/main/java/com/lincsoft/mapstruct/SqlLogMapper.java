package com.lincsoft.mapstruct;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.SqlLogDetailResponse;
import com.lincsoft.controller.log.vo.SqlLogPageResponseItem;
import com.lincsoft.entity.system.SysSqlLog;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * SQL Log mapper for converting between VO and SysSqlLog.
 *
 * @author 林创科技
 * @since 2026-06-11
 */
@Mapper(componentModel = "spring")
public interface SqlLogMapper {
  /**
   * Convert SysSqlLog to SqlLogPageResponseItem.
   *
   * @param entity SysSqlLog entity
   * @return SqlLogPageResponseItem VO
   */
  @Mapping(target = "createdAt", source = "createTime")
  SqlLogPageResponseItem toPageResponseItem(SysSqlLog entity);

  /**
   * Convert IPage of SysSqlLog to IPage of SqlLogPageResponseItem.
   *
   * @param entities IPage of SysSqlLog entities
   * @return IPage of SqlLogPageResponseItem VO
   */
  default IPage<SqlLogPageResponseItem> toPageResponse(IPage<SysSqlLog> entities) {
    return entities.convert(this::toPageResponseItem);
  }

  /**
   * Convert SysSqlLog to SqlLogDetailResponse.
   *
   * @param entity SysSqlLog entity
   * @return SqlLogDetailResponse VO
   */
  SqlLogDetailResponse toDetailResponse(SysSqlLog entity);

  /**
   * Convert list of SysSqlLog to list of SqlLogDetailResponse.
   *
   * @param entities List of SysSqlLog entities
   * @return List of SqlLogDetailResponse
   */
  List<SqlLogDetailResponse> toDetailResponseList(List<SysSqlLog> entities);
}
