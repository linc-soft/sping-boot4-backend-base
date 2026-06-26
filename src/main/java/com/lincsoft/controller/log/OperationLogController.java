package com.lincsoft.controller.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.OperationLogDetailResponse;
import com.lincsoft.controller.log.vo.OperationLogPageRequest;
import com.lincsoft.controller.log.vo.OperationLogPageResponseItem;
import com.lincsoft.mapstruct.OperationLogQueryMapper;
import com.lincsoft.services.system.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Operation Log Controller.
 *
 * <p>Provides endpoints for operation log management.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Tag(name = "Operation Log", description = "Operation log management API")
@RestController
@RequestMapping("/api/logs/operation")
@RequiredArgsConstructor
public class OperationLogController {

  private final OperationLogService operationLogService;
  private final OperationLogQueryMapper operationLogMapper;

  /**
   * Get operation log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of operation log items
   */
  @Operation(
      summary = "Get operation log page",
      description = "Query operation logs with pagination, sorted by time descending")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LIST_LOG.roleCode)")
  @GetMapping("/page")
  public IPage<OperationLogPageResponseItem> getPage(@Valid OperationLogPageRequest request) {
    return operationLogMapper.toPageResponse(operationLogService.getPage(request));
  }

  /**
   * Get operation log detail by ID.
   *
   * @param id Operation log ID
   * @return Operation log detail response
   */
  @Operation(
      summary = "Get operation log detail",
      description = "Retrieve complete operation log information by ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).VIEW_LOG.roleCode)")
  @GetMapping("/{id}")
  public OperationLogDetailResponse getById(
      @Parameter(description = "Log ID") @PathVariable Long id) {
    return operationLogMapper.toDetailResponse(operationLogService.getById(id));
  }

  /**
   * Get operation log list by trace ID.
   *
   * @param traceId Trace ID
   * @return List of operation log detail responses
   */
  @Operation(
      summary = "Get operation logs by TraceId",
      description = "Retrieve all operation logs by trace ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).VIEW_LOG.roleCode)")
  @GetMapping("/trace/{traceId}")
  public List<OperationLogDetailResponse> getListByTraceId(
      @Parameter(description = "Trace ID") @PathVariable String traceId) {
    return operationLogMapper.toDetailResponseList(operationLogService.getListByTraceId(traceId));
  }
}
