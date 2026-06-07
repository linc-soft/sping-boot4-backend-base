package com.lincsoft.mapstruct;

import com.lincsoft.controller.oa.vo.LeaveInfoResponse;
import com.lincsoft.controller.oa.vo.LeavePageResponseItem;
import com.lincsoft.controller.oa.vo.LeaveSubmitRequest;
import com.lincsoft.entity.oa.OaLeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Leave request mapper for converting between leave VOs and OaLeaveRequest.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {
  /**
   * Convert LeaveSubmitRequest to OaLeaveRequest.
   *
   * <p>Framework-managed fields and workflow-managed fields (employeeId, status, processInstanceId,
   * approverId, approvalComment) are intentionally ignored: they are populated by the service.
   *
   * @param request Leave submit request
   * @return OaLeaveRequest entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "employeeId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "processInstanceId", ignore = true)
  @Mapping(target = "approverId", ignore = true)
  @Mapping(target = "approvalComment", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  OaLeaveRequest toEntity(LeaveSubmitRequest request);

  /**
   * Convert OaLeaveRequest to LeaveInfoResponse.
   *
   * @param entity OaLeaveRequest entity
   * @return LeaveInfoResponse VO
   */
  LeaveInfoResponse toInfoResponse(OaLeaveRequest entity);

  /**
   * Convert OaLeaveRequest to LeavePageResponseItem.
   *
   * @param entity OaLeaveRequest entity
   * @return LeavePageResponseItem VO
   */
  LeavePageResponseItem toPageResponseItem(OaLeaveRequest entity);
}
