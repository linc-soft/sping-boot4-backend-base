package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.OperationLogPageRequest;
import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysOperationLogMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Operation Log Service.
 *
 * <p>Provides query operations and async persistence for operation logs.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
  private final SysOperationLogMapper operationLogMapper;

  // ==================== Query Operations ====================

  /**
   * Get operation log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of operation log entities
   */
  public IPage<SysOperationLog> getPage(OperationLogPageRequest request) {
    QueryWrapper<SysOperationLog> queryWrapper = buildQueryWrapper(request);
    request.applySorting(
        queryWrapper,
        Set.of(
            "id",
            "trace_id",
            "module",
            "sub_module",
            "operation_type",
            "description",
            "duration",
            "request_method",
            "request_url",
            "client_ip",
            "username",
            "create_time"),
        "create_time");
    return operationLogMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get operation log by ID.
   *
   * @param id Operation log ID
   * @return Operation log entity
   * @throws BusinessException if the log is not found
   */
  public SysOperationLog getById(Long id) {
    SysOperationLog entity = operationLogMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("Operation log not found with id: " + id);
    }
    return entity;
  }

  /**
   * Get operation log list by trace ID.
   *
   * @param traceId Trace ID
   * @return List of operation log entities
   */
  public List<SysOperationLog> getListByTraceId(String traceId) {
    QueryWrapper<SysOperationLog> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("trace_id", traceId);
    queryWrapper.orderByAsc("create_time");
    return operationLogMapper.selectList(queryWrapper);
  }

  // ==================== Write Operations ====================

  /**
   * Saves operation log asynchronously.
   *
   * <p>Executed in the {@code asyncExecutor} thread pool. If an exception occurs during
   * persistence, it is logged but not rethrown to avoid affecting the main request flow.
   *
   * @param operationLog the operation log entity to persist
   */
  @Async("asyncExecutor")
  public void save(SysOperationLog operationLog) {
    try {
      operationLogMapper.insert(operationLog);
    } catch (Exception e) {
      log.error("Failed to save operation log: {}", e.getMessage(), e);
    }
  }

  // ==================== Private Methods ====================

  /**
   * Build query wrapper from request.
   *
   * @param request Query request
   * @return QueryWrapper
   */
  private QueryWrapper<SysOperationLog> buildQueryWrapper(OperationLogPageRequest request) {
    QueryWrapper<SysOperationLog> queryWrapper = new QueryWrapper<>();

    if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
      queryWrapper.eq("trace_id", request.getTraceId());
    }

    if (request.getOperationType() != null) {
      queryWrapper.eq("operation_type", request.getOperationType().name());
    }

    if (request.getModule() != null && !request.getModule().isBlank()) {
      queryWrapper.eq("module", request.getModule());
    }

    if (request.getSubModule() != null && !request.getSubModule().isBlank()) {
      queryWrapper.eq("sub_module", request.getSubModule());
    }

    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      queryWrapper.like("username", request.getUsername());
    }

    if (request.getStartTime() != null) {
      queryWrapper.ge("create_time", request.getStartTime());
    }

    if (request.getEndTime() != null) {
      queryWrapper.le("create_time", request.getEndTime());
    }

    return queryWrapper;
  }
}
