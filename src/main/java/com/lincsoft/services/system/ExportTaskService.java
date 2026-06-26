package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.ExportStatusEnums;
import com.lincsoft.constant.ExportTypeEnums;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.controller.log.vo.LogExportRequest;
import com.lincsoft.controller.log.vo.export.AccessLogExportItem;
import com.lincsoft.controller.log.vo.export.ErrorLogExportItem;
import com.lincsoft.controller.log.vo.export.OperationLogExportItem;
import com.lincsoft.controller.log.vo.export.SqlLogExportItem;
import com.lincsoft.entity.system.SysAccessLog;
import com.lincsoft.entity.system.SysErrorLog;
import com.lincsoft.entity.system.SysExportTask;
import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.entity.system.SysSqlLog;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysAccessLogMapper;
import com.lincsoft.mapper.system.SysErrorLogMapper;
import com.lincsoft.mapper.system.SysExportTaskMapper;
import com.lincsoft.mapper.system.SysOperationLogMapper;
import com.lincsoft.mapper.system.SysSqlLogMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Export Task Service.
 *
 * <p>Provides async export task management with cursor-based streaming NDJSON+Gzip generation.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskService {

  private static final int CURSOR_BATCH_SIZE = 500;

  private final SysExportTaskMapper exportTaskMapper;
  private final SysAccessLogMapper accessLogMapper;
  private final SysOperationLogMapper operationLogMapper;
  private final SysSqlLogMapper sqlLogMapper;
  private final SysErrorLogMapper errorLogMapper;
  private final AppProperties appProperties;

  @Lazy private final ExportTaskService self;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  /**
   * Create or reuse an export task for the given user and type.
   *
   * @param request query filters for access logs
   * @param username current user
   * @return taskId of the created or existing task
   */
  public String createOrReuseTask(LogExportRequest request, String username) {
    QueryWrapper<SysExportTask> queryWrapper = new QueryWrapper<>();
    queryWrapper
        .eq("created_by", username)
        .eq("type", ExportTypeEnums.LOG_TRACE.name())
        .in("status", List.of(ExportStatusEnums.PENDING.name(), ExportStatusEnums.RUNNING.name()));
    SysExportTask existing = exportTaskMapper.selectOne(queryWrapper);
    if (existing != null) {
      return existing.getTaskId();
    }

    String taskId = UUID.randomUUID().toString();
    SysExportTask task = new SysExportTask();
    task.setTaskId(taskId);
    task.setType(ExportTypeEnums.LOG_TRACE.name());
    task.setStatus(ExportStatusEnums.PENDING.name());
    task.setCreatedBy(username);
    task.setCreatedAt(LocalDateTime.now());
    exportTaskMapper.insert(task);

    self.performExportAsync(taskId, request);
    return taskId;
  }

  /**
   * Get export task with permission check.
   *
   * @param taskId task identifier
   * @param currentUser current user
   * @return export task entity
   * @throws BusinessException if not found or forbidden
   */
  public SysExportTask getTask(String taskId, String currentUser) {
    QueryWrapper<SysExportTask> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("task_id", taskId);
    SysExportTask task = exportTaskMapper.selectOne(queryWrapper);
    if (task == null) {
      throw new BusinessException(MessageEnums.SYS_EXPORT_TASK_NOT_FOUND);
    }
    if (!task.getCreatedBy().equals(currentUser)) {
      throw new BusinessException(MessageEnums.SYS_EXPORT_TASK_FORBIDDEN);
    }
    return task;
  }

  /**
   * Resolve the file path for download, validating status and expiration.
   *
   * @param taskId task identifier
   * @param currentUser current user
   * @return resolved file path
   * @throws BusinessException if not ready or expired
   */
  public Path resolveFilePath(String taskId, String currentUser) {
    SysExportTask task = getTask(taskId, currentUser);
    if (!ExportStatusEnums.SUCCESS.name().equals(task.getStatus())) {
      throw new BusinessException(MessageEnums.SYS_EXPORT_FILE_NOT_FOUND);
    }
    if (task.getExpireAt() != null && task.getExpireAt().isBefore(LocalDateTime.now())) {
      task.setStatus(ExportStatusEnums.EXPIRED.name());
      exportTaskMapper.updateById(task);
      throw new BusinessException(MessageEnums.SYS_EXPORT_FILE_EXPIRED);
    }
    Path exportDir = Path.of(appProperties.getExport().getDirectory());
    Path filePath = exportDir.resolve(task.getFilePath());
    if (!Files.exists(filePath)) {
      throw new BusinessException(MessageEnums.SYS_EXPORT_FILE_NOT_FOUND);
    }
    return filePath;
  }

  /**
   * Delete an export task and its file.
   *
   * @param taskId task identifier
   * @param currentUser current user
   */
  public void deleteTask(String taskId, String currentUser) {
    SysExportTask task = getTask(taskId, currentUser);
    if (task.getFilePath() != null) {
      try {
        Path exportDir = Path.of(appProperties.getExport().getDirectory());
        Files.deleteIfExists(exportDir.resolve(task.getFilePath()));
      } catch (IOException e) {
        log.warn("Failed to delete export file for task {}: {}", taskId, e.getMessage());
      }
    }
    exportTaskMapper.deleteById(task.getId());
  }

  /** Scheduled cleanup of expired export tasks. Runs every hour. */
  @Scheduled(cron = "0 0 * * * ?")
  public void cleanupExpiredTasks() {
    QueryWrapper<SysExportTask> queryWrapper = new QueryWrapper<>();
    queryWrapper
        .eq("status", ExportStatusEnums.SUCCESS.name())
        .lt("expire_at", LocalDateTime.now());
    List<SysExportTask> expiredTasks = exportTaskMapper.selectList(queryWrapper);
    if (expiredTasks.isEmpty()) {
      return;
    }
    Path exportDir = Path.of(appProperties.getExport().getDirectory());
    int cleaned = 0;
    for (SysExportTask task : expiredTasks) {
      if (task.getFilePath() != null) {
        try {
          Files.deleteIfExists(exportDir.resolve(task.getFilePath()));
        } catch (IOException e) {
          log.warn(
              "Failed to delete expired export file {}: {}", task.getFilePath(), e.getMessage());
        }
      }
      task.setStatus(ExportStatusEnums.EXPIRED.name());
      exportTaskMapper.updateById(task);
      cleaned++;
    }
    log.info("Cleaned up {} expired export tasks", cleaned);
  }

  // ==================== Async Export ====================

  @Async("asyncExecutor")
  void performExportAsync(String taskId, LogExportRequest request) {
    Path tempFile = null;
    try {
      updateStatus(taskId, ExportStatusEnums.RUNNING, null);

      Path exportDir = Path.of(appProperties.getExport().getDirectory());
      String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
      Path dateDir = exportDir.resolve(datePath);
      Files.createDirectories(dateDir);

      tempFile = dateDir.resolve(taskId + ".jsonl.gz.tmp");

      int totalCount;
      try (OutputStream fos = Files.newOutputStream(tempFile);
          GZIPOutputStream gzos = new GZIPOutputStream(fos);
          JsonGenerator gen = objectMapper.getFactory().createGenerator(gzos)) {

        totalCount = streamExport(request, gen);
      }

      String finalFileName = taskId + ".jsonl.gz";
      Path finalFile = dateDir.resolve(finalFileName);
      Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE);

      String userFileName =
          "log_trace_export_"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
              + ".jsonl.gz";

      SysExportTask task = loadTask(taskId);
      task.setStatus(ExportStatusEnums.SUCCESS.name());
      task.setFilePath(datePath + "/" + finalFileName);
      task.setFileName(userFileName);
      task.setFileSize(Files.size(finalFile));
      task.setRowCount(totalCount);
      task.setCompletedAt(LocalDateTime.now());
      task.setExpireAt(LocalDateTime.now().plusHours(appProperties.getExport().getTtlHours()));
      exportTaskMapper.updateById(task);

      log.info(
          "Export task {} completed: {} records, {} bytes", taskId, totalCount, task.getFileSize());
    } catch (Exception e) {
      log.error("Export task {} failed: {}", taskId, e.getMessage(), e);
      try {
        if (tempFile != null) {
          Files.deleteIfExists(tempFile);
        }
      } catch (IOException ignored) {
      }
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.length() > 1000) {
        errorMsg = errorMsg.substring(0, 1000);
      }
      updateStatus(taskId, ExportStatusEnums.FAILED, errorMsg);
    }
  }

  private int streamExport(LogExportRequest request, JsonGenerator gen) throws IOException {
    int totalCount = 0;
    long lastId = 0;

    while (true) {
      QueryWrapper<SysAccessLog> queryWrapper = buildAccessLogQuery(request, lastId);
      List<SysAccessLog> batch = accessLogMapper.selectList(queryWrapper);
      if (batch.isEmpty()) {
        break;
      }

      totalCount += batch.size();
      if (totalCount > appProperties.getExport().getMaxRecords()) {
        gen.flush();
        throw new IllegalStateException(
            "Export exceeds maximum record limit ("
                + appProperties.getExport().getMaxRecords()
                + ")");
      }

      List<String> traceIds =
          batch.stream().map(SysAccessLog::getTraceId).filter(t -> t != null).distinct().toList();

      Map<String, List<SysOperationLog>> opMap = Collections.emptyMap();
      Map<String, List<SysSqlLog>> sqlMap = Collections.emptyMap();
      Map<String, SysErrorLog> errMap = Collections.emptyMap();

      if (!traceIds.isEmpty()) {
        opMap =
            operationLogMapper
                .selectList(new QueryWrapper<SysOperationLog>().in("trace_id", traceIds))
                .stream()
                .collect(Collectors.groupingBy(SysOperationLog::getTraceId));

        sqlMap =
            sqlLogMapper.selectList(new QueryWrapper<SysSqlLog>().in("trace_id", traceIds)).stream()
                .collect(Collectors.groupingBy(SysSqlLog::getTraceId));

        errMap =
            errorLogMapper
                .selectList(new QueryWrapper<SysErrorLog>().in("trace_id", traceIds))
                .stream()
                .collect(Collectors.toMap(SysErrorLog::getTraceId, e -> e, (e1, e2) -> e1));
      }

      for (SysAccessLog accessLog : batch) {
        String traceId = accessLog.getTraceId();
        gen.writeStartObject();
        gen.writeStringField("traceId", traceId);

        gen.writeObjectField("accessLog", toAccessLogExportItem(accessLog));

        List<SysOperationLog> ops = opMap.getOrDefault(traceId, List.of());
        List<OperationLogExportItem> opItems = new ArrayList<>();
        for (SysOperationLog op : ops) {
          opItems.add(toOperationLogExportItem(op));
        }
        gen.writeObjectField("operationLogs", opItems);

        List<SysSqlLog> sqls = sqlMap.getOrDefault(traceId, List.of());
        List<SqlLogExportItem> sqlItems = new ArrayList<>();
        for (SysSqlLog sql : sqls) {
          sqlItems.add(toSqlLogExportItem(sql));
        }
        gen.writeObjectField("sqlLogs", sqlItems);

        SysErrorLog err = errMap.get(traceId);
        if (err != null) {
          gen.writeObjectField("errorLog", toErrorLogExportItem(err));
        } else {
          gen.writeNullField("errorLog");
        }

        gen.writeEndObject();
        gen.writeRaw('\n');
      }

      lastId = batch.get(batch.size() - 1).getId();
    }

    gen.flush();
    return totalCount;
  }

  private QueryWrapper<SysAccessLog> buildAccessLogQuery(LogExportRequest request, long lastId) {
    QueryWrapper<SysAccessLog> queryWrapper = new QueryWrapper<>();

    if (lastId > 0) {
      queryWrapper.gt("id", lastId);
    }
    if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
      queryWrapper.eq("trace_id", request.getTraceId());
    }
    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      queryWrapper.like("username", request.getUsername());
    }
    if (request.getMethod() != null && !request.getMethod().isBlank()) {
      queryWrapper.eq("request_method", request.getMethod());
    }
    if (request.getPath() != null && !request.getPath().isBlank()) {
      queryWrapper.like("request_url", request.getPath());
    }
    if (request.getStatusCode() != null && !request.getStatusCode().isBlank()) {
      String sc = request.getStatusCode().toUpperCase();
      if ("2XX".equals(sc)) {
        queryWrapper.apply("response_status % 1000 >= 200 AND response_status % 1000 < 300");
      } else if ("4XX".equals(sc)) {
        queryWrapper.apply("response_status % 1000 >= 400 AND response_status % 1000 < 500");
      } else if ("5XX".equals(sc)) {
        queryWrapper.apply("response_status % 1000 >= 500 AND response_status % 1000 < 600");
      } else {
        try {
          queryWrapper.eq("response_status", Integer.parseInt(sc));
        } catch (NumberFormatException ignored) {
        }
      }
    }
    if (request.getStartTime() != null) {
      queryWrapper.ge("create_time", request.getStartTime());
    }
    if (request.getEndTime() != null) {
      queryWrapper.le("create_time", request.getEndTime());
    }
    queryWrapper.orderByAsc("id");
    queryWrapper.last("LIMIT " + CURSOR_BATCH_SIZE);
    return queryWrapper;
  }

  // ==================== DTO Mapping ====================

  private AccessLogExportItem toAccessLogExportItem(SysAccessLog e) {
    return new AccessLogExportItem(
        e.getRequestMethod(),
        e.getRequestUrl(),
        e.getRequestParams(),
        parseJsonText(e.getRequestHeaders()),
        parseJsonText(e.getRequestBody()),
        e.getResponseStatus(),
        parseJsonText(e.getResponseHeaders()),
        parseJsonText(e.getResponseBody()),
        e.getClientIp(),
        e.getUserAgent(),
        e.getUsername(),
        e.getDuration(),
        e.getCreateTime());
  }

  private Object parseJsonText(String jsonText) {
    if (jsonText == null || jsonText.isBlank()) {
      return jsonText;
    }
    try {
      return objectMapper.readTree(jsonText);
    } catch (Exception e) {
      return jsonText;
    }
  }

  private OperationLogExportItem toOperationLogExportItem(SysOperationLog e) {
    return new OperationLogExportItem(
        e.getModule(),
        e.getSubModule(),
        e.getOperationType(),
        e.getDescription(),
        e.getDuration(),
        e.getCreateTime());
  }

  private SqlLogExportItem toSqlLogExportItem(SysSqlLog e) {
    return new SqlLogExportItem(
        e.getSqlType(),
        e.getSqlText(),
        e.getSqlParams(),
        e.getMapperClass(),
        e.getMapperMethod(),
        e.getRowCount(),
        e.getDuration(),
        e.getCreateTime());
  }

  private ErrorLogExportItem toErrorLogExportItem(SysErrorLog e) {
    return new ErrorLogExportItem(
        e.getExceptionFile(),
        e.getExceptionClass(),
        e.getExceptionMethod(),
        e.getExceptionLine(),
        e.getExceptionMessage(),
        e.getRootCauseMessage(),
        e.getStackTrace(),
        e.getCreateTime());
  }

  // ==================== Helpers ====================

  private void updateStatus(String taskId, ExportStatusEnums status, String errorMessage) {
    SysExportTask task = loadTask(taskId);
    task.setStatus(status.name());
    if (errorMessage != null) {
      task.setErrorMessage(errorMessage);
    }
    exportTaskMapper.updateById(task);
  }

  private SysExportTask loadTask(String taskId) {
    QueryWrapper<SysExportTask> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("task_id", taskId);
    return exportTaskMapper.selectOne(queryWrapper);
  }
}
