package com.lincsoft.mapstruct;

import com.lincsoft.controller.common.vo.FileMetadataResponse;
import com.lincsoft.controller.leave.vo.LeavePageResponse;
import com.lincsoft.entity.leave.Leave;
import com.lincsoft.entity.system.SysFileUpload;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Leave mapper for converting between VO and entity.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Mapper(componentModel = "spring")
public interface LeaveMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "approverNickname", ignore = true)
  @Mapping(target = "files", ignore = true)
  @Mapping(target = "nickname", ignore = true)
  LeavePageResponse toPageResponse(Leave entity);

  @Mapping(target = "size", source = "fileSize")
  FileMetadataResponse toFileMetadataResponse(SysFileUpload entity);

  List<FileMetadataResponse> toFileMetadataResponseList(List<SysFileUpload> entities);
}
