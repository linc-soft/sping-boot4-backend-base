package com.lincsoft.controller.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.AccessLogDetailResponse;
import com.lincsoft.controller.log.vo.AccessLogPageRequest;
import com.lincsoft.controller.log.vo.AccessLogPageResponseItem;
import com.lincsoft.mapstruct.AccessLogMapper;
import com.lincsoft.services.system.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Access Log Controller.
 *
 * <p>Provides endpoints for access log management.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Tag(name = "Access Log", description = "Access log management API")
@RestController
@RequestMapping("/api/logs/access")
@RequiredArgsConstructor
public class AccessLogController {

  private final AccessLogService accessLogService;
  private final AccessLogMapper accessLogMapper;

  /**
   * Get access log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of access log items
   */
  @Operation(
      summary = "Get access log page",
      description = "Query access logs with pagination, sorted by time descending")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LIST_LOG.roleCode)")
  @GetMapping("/page")
  public IPage<AccessLogPageResponseItem> getPage(@Valid AccessLogPageRequest request) {
    return accessLogMapper.toPageResponse(accessLogService.getPage(request));
  }

  /**
   * Get access log detail by ID.
   *
   * @param id Access log ID
   * @return Access log detail response
   */
  @Operation(
      summary = "Get access log detail",
      description = "Retrieve complete access log information by ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).VIEW_LOG.roleCode)")
  @GetMapping("/{id}")
  public AccessLogDetailResponse getById(@Parameter(description = "Log ID") @PathVariable Long id) {
    return accessLogMapper.toDetailResponse(accessLogService.getById(id));
  }

  /**
   * Get access log detail by trace ID.
   *
   * @param traceId Trace ID
   * @return Access log detail response
   */
  @Operation(summary = "Get access log by TraceId", description = "Retrieve access log by trace ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).VIEW_LOG.roleCode)")
  @GetMapping("/trace/{traceId}")
  public AccessLogDetailResponse getByTraceId(
      @Parameter(description = "Trace ID") @PathVariable String traceId) {
    return accessLogMapper.toDetailResponse(accessLogService.getByTraceId(traceId));
  }
}
