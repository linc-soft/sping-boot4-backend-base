package com.lincsoft.controller.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.ErrorLogDetailResponse;
import com.lincsoft.controller.log.vo.ErrorLogPageRequest;
import com.lincsoft.controller.log.vo.ErrorLogPageResponseItem;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.mapstruct.ErrorLogMapper;
import com.lincsoft.services.system.ErrorLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Error Log Controller.
 *
 * <p>Provides endpoints for error log management.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Tag(name = "Error Log", description = "Error log management API")
@RestController
@RequestMapping("/api/logs/error")
@RequiredArgsConstructor
public class ErrorLogController {

  private final ErrorLogService errorLogService;
  private final ErrorLogMapper errorLogMapper;

  /**
   * Get error log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of error log items
   */
  @Operation(
      summary = "Get error log page",
      description = "Query error logs with pagination, sorted by time descending")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_VIEW.roleCode)")
  @GetMapping("/page")
  public IPage<ErrorLogPageResponseItem> getPage(@Valid ErrorLogPageRequest request) {
    return errorLogMapper.toPageResponse(errorLogService.getPage(request));
  }

  /**
   * Get error log detail by ID.
   *
   * @param id Error log ID
   * @return Error log detail response
   */
  @Operation(
      summary = "Get error log detail",
      description = "Retrieve complete error log information by ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_VIEW.roleCode)")
  @GetMapping("/{id}")
  public ErrorLogDetailResponse getById(@Parameter(description = "Log ID") @PathVariable Long id) {
    return errorLogMapper.toDetailResponse(errorLogService.getById(id));
  }

  /**
   * Get error log detail by trace ID.
   *
   * @param traceId Trace ID
   * @return Error log detail response, or null if not found
   */
  @Operation(
      summary = "Get error log by TraceId",
      description = "Retrieve error log by trace ID, returns null if not found")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_VIEW.roleCode)")
  @GetMapping("/trace/{traceId}")
  public ErrorLogDetailResponse getByTraceId(
      @Parameter(description = "Trace ID") @PathVariable String traceId) {
    SysErrorLog entity = errorLogService.getByTraceId(traceId);
    return entity != null ? errorLogMapper.toDetailResponse(entity) : null;
  }
}
