package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.log.vo.ErrorLogPageRequest;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysErrorLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Error Log Service.
 *
 * <p>Provides query operations and async persistence for error logs.
 *
 * @author 林创科技
 * @since 2026-05-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorLogService {
  private final SysErrorLogMapper errorLogMapper;

  // ==================== Query Operations ====================

  /**
   * Get error log page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of error log entities
   */
  public IPage<SysErrorLog> getPage(ErrorLogPageRequest request) {
    QueryWrapper<SysErrorLog> queryWrapper = buildQueryWrapper(request);
    queryWrapper.orderByDesc("create_time");
    return errorLogMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get error log by ID.
   *
   * @param id Error log ID
   * @return Error log entity
   * @throws BusinessException if the log is not found
   */
  public SysErrorLog getById(Long id) {
    SysErrorLog entity = errorLogMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("Error log not found with id: " + id);
    }
    return entity;
  }

  /**
   * Get error log by trace ID.
   *
   * @param traceId Trace ID
   * @return Error log entity, or null if not found
   */
  public SysErrorLog getByTraceId(String traceId) {
    QueryWrapper<SysErrorLog> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("trace_id", traceId);
    return errorLogMapper.selectOne(queryWrapper);
  }

  // ==================== Write Operations ====================

  /**
   * Asynchronously saves the error log.
   *
   * <p>Executed asynchronously using the {@code asyncExecutor} thread pool. If an exception occurs
   * during the save operation, it will be logged and not re-thrown.
   *
   * @param errorLog The error log entity to be saved.
   */
  @Async("asyncExecutor")
  public void save(SysErrorLog errorLog) {
    try {
      errorLogMapper.insert(errorLog);
    } catch (Exception e) {
      log.error("Failed to save error log: {}", e.getMessage(), e);
    }
  }

  // ==================== Private Methods ====================

  /**
   * Build query wrapper from request.
   *
   * @param request Query request
   * @return QueryWrapper
   */
  private QueryWrapper<SysErrorLog> buildQueryWrapper(ErrorLogPageRequest request) {
    QueryWrapper<SysErrorLog> queryWrapper = new QueryWrapper<>();

    if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
      queryWrapper.eq("trace_id", request.getTraceId());
    }

    // Keyword fuzzy search across multiple fields (OR condition)
    if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
      String keyword = request.getKeyword();
      queryWrapper.and(
          wrapper ->
              wrapper
                  .like("exception_file", keyword)
                  .or()
                  .like("exception_class", keyword)
                  .or()
                  .like("exception_method", keyword)
                  .or()
                  .like("exception_message", keyword)
                  .or()
                  .like("root_cause_message", keyword)
                  .or()
                  .like("stack_trace", keyword));
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
