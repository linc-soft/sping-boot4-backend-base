package com.lincsoft.controller.log;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.config.AppProperties;
import com.lincsoft.controller.log.vo.SqlLogDetailResponse;
import com.lincsoft.controller.log.vo.SqlLogPageRequest;
import com.lincsoft.controller.log.vo.SqlLogPageResponseItem;
import com.lincsoft.mapstruct.SqlLogMapper;
import com.lincsoft.services.system.SqlLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * SQL Log Controller.
 *
 * <p>Provides endpoints for SQL log management.
 *
 * @author 林创科技
 * @since 2026-06-11
 */
@Tag(name = "SQL Log", description = "SQL log management API")
@RestController
@RequestMapping("/api/logs/sql")
@RequiredArgsConstructor
public class SqlLogController {

  private final SqlLogService sqlLogService;
  private final SqlLogMapper sqlLogMapper;
  private final AppProperties appProperties;

  /**
   * Get SQL log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of SQL log items
   */
  @Operation(
      summary = "Get SQL log page",
      description = "Query SQL logs with pagination, sorted by time descending")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_READ.roleCode)")
  @GetMapping("/page")
  public IPage<SqlLogPageResponseItem> getPage(@Valid SqlLogPageRequest request) {
    IPage<SqlLogPageResponseItem> page =
        sqlLogMapper.toPageResponse(sqlLogService.getPage(request));
    long threshold = appProperties.getSqlLog().getSlowSqlThresholdMs();
    if (threshold > 0) {
      for (int i = 0; i < page.getRecords().size(); i++) {
        SqlLogPageResponseItem item = page.getRecords().get(i);
        if (item.duration() != null) {
          page.getRecords()
              .set(
                  i,
                  new SqlLogPageResponseItem(
                      item.id(),
                      item.traceId(),
                      item.sqlType(),
                      item.mapperMethod(),
                      item.duration(),
                      item.username(),
                      item.duration() > threshold,
                      item.createdAt()));
        }
      }
    }
    return page;
  }

  /**
   * Get SQL log detail by ID.
   *
   * @param id SQL log ID
   * @return SQL log detail response
   */
  @Operation(
      summary = "Get SQL log detail",
      description = "Retrieve complete SQL log information by ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LOG_READ.roleCode)")
  @GetMapping("/{id}")
  public SqlLogDetailResponse getById(@Parameter(description = "Log ID") @PathVariable Long id) {
    return sqlLogMapper.toDetailResponse(sqlLogService.getById(id));
  }
}
