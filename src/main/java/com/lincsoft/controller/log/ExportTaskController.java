package com.lincsoft.controller.log;

import com.lincsoft.controller.log.vo.ExportTaskResponse;
import com.lincsoft.controller.log.vo.LogExportRequest;
import com.lincsoft.entity.system.SysExportTask;
import com.lincsoft.services.system.ExportTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Export Task Controller.
 *
 * <p>Provides endpoints for async export task management and file download.
 *
 * @author 林创科技
 * @since 2026-06-26
 */
@Tag(name = "Export Task", description = "Async export task management API")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class ExportTaskController {

  private final ExportTaskService exportTaskService;

  @Operation(
      summary = "Create export task",
      description = "Create or reuse an async log export task")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EXPORT_LOG.roleCode)")
  @PostMapping("/export-logs")
  public Map<String, String> createTask(@RequestBody LogExportRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    String taskId = exportTaskService.createOrReuseTask(request, username);
    return Map.of("taskId", taskId);
  }

  @Operation(summary = "Get export task", description = "Query export task status by task ID")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EXPORT_LOG.roleCode)")
  @GetMapping("/{taskId}")
  public ExportTaskResponse getTask(
      @Parameter(description = "Task ID") @PathVariable String taskId) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    SysExportTask task = exportTaskService.getTask(taskId, username);
    return new ExportTaskResponse(
        task.getTaskId(),
        task.getType(),
        task.getStatus(),
        task.getFileName(),
        task.getFileSize(),
        task.getRowCount(),
        task.getCompletedAt(),
        task.getExpireAt(),
        task.getErrorMessage(),
        task.getCreatedBy(),
        task.getCreatedAt());
  }

  @Operation(summary = "Download export file", description = "Download the generated export file")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EXPORT_LOG.roleCode)")
  @GetMapping("/{taskId}/download")
  public ResponseEntity<StreamingResponseBody> download(
      @Parameter(description = "Task ID") @PathVariable String taskId) throws IOException {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    Path filePath = exportTaskService.resolveFilePath(taskId, username);
    SysExportTask task = exportTaskService.getTask(taskId, username);

    String encodedFilename =
        URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

    StreamingResponseBody body = outputStream -> Files.copy(filePath, outputStream);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
        .header("filename", encodedFilename)
        .contentType(MediaType.parseMediaType("application/gzip"))
        .contentLength(task.getFileSize())
        .body(body);
  }

  @Operation(summary = "Delete export task", description = "Delete an export task and its file")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).EXPORT_LOG.roleCode)")
  @DeleteMapping("/{taskId}")
  public void deleteTask(@Parameter(description = "Task ID") @PathVariable String taskId) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    exportTaskService.deleteTask(taskId, username);
  }
}
