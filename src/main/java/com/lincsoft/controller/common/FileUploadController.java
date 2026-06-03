package com.lincsoft.controller.common;

import com.lincsoft.annotation.IgnoreResultWrapper;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.entity.system.SysFileUpload;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.services.system.FileUploadService;
import com.lincsoft.services.system.FileUploadService.FileMetadata;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * File upload controller.
 *
 * <p>Provides endpoints for uploading, downloading, verifying, and deleting files. Files are stored
 * on the local filesystem under the configured upload directory. Upload metadata is persisted to
 * the {@code sys_file_upload} table.
 *
 * @author 林创科技
 * @since 2026-06-03
 */
@Slf4j
@RestController
@RequestMapping("/api/common/files")
@RequiredArgsConstructor
public class FileUploadController {

  private final FileUploadService fileUploadService;

  /**
   * Upload a single file.
   *
   * @param file the multipart file to upload
   * @param associateType optional association type (e.g., "USER_AVATAR")
   * @param associateId optional associated business entity ID
   * @return metadata of the stored file, including MD5 hash
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).FILE_WRITE.roleCode)")
  @PostMapping
  public FileMetadata upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "associateType", required = false) String associateType,
      @RequestParam(value = "associateId", required = false) Long associateId) {
    return fileUploadService.upload(file, associateType, associateId);
  }

  /**
   * Upload multiple files.
   *
   * @param files the multipart files to upload
   * @param associateType optional association type applied to all files
   * @param associateId optional associated business entity ID applied to all files
   * @return list of metadata for each stored file
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).FILE_WRITE.roleCode)")
  @PostMapping("/batch")
  public List<FileMetadata> uploadBatch(
      @RequestParam("files") MultipartFile[] files,
      @RequestParam(value = "associateType", required = false) String associateType,
      @RequestParam(value = "associateId", required = false) Long associateId) {
    return Arrays.stream(files)
        .map(f -> fileUploadService.upload(f, associateType, associateId))
        .toList();
  }

  /**
   * Download a file by its date and stored name.
   *
   * <p>The response includes the {@code X-File-MD5} header containing the MD5 hash stored in the
   * database, allowing the client to verify file integrity.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   * @return the file content as a downloadable response
   * @throws IOException if the file resource cannot be read
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).FILE_READ.roleCode)")
  @IgnoreResultWrapper
  @GetMapping("/{dateUrl}/{storedName}")
  public ResponseEntity<Resource> download(
      @PathVariable String dateUrl, @PathVariable String storedName) throws IOException {
    Path filePath = fileUploadService.resolveFilePath(dateUrl, storedName);

    // Verify file integrity before serving
    SysFileUpload record = fileUploadService.findRecord(storedName);
    if (record != null && record.getMd5() != null) {
      if (!fileUploadService.verifyMd5(dateUrl, storedName)) {
        log.error("MD5 mismatch for file: storedName={}", storedName);
        throw new BusinessException(MessageEnums.SYS_FILE_MD5_MISMATCH);
      }
    }

    Resource resource = new UrlResource(filePath.toUri());

    String encodedFilename =
        URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8);

    ResponseEntity.BodyBuilder builder =
        ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + encodedFilename)
            .header("filename", encodedFilename)
            .contentType(MediaType.APPLICATION_OCTET_STREAM);

    // Attach stored MD5 hash so the client can verify integrity
    if (record != null && record.getMd5() != null) {
      builder.header("X-File-MD5", record.getMd5());
    }

    return builder.body(resource);
  }

  /**
   * Verify that the file on disk matches the stored MD5 hash in the database.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   * @return {@code {"match": true/false, "storedMd5": "...", "currentMd5": "..."}}
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).FILE_READ.roleCode)")
  @GetMapping("/{dateUrl}/{storedName}/verify")
  public Map<String, Object> verify(@PathVariable String dateUrl, @PathVariable String storedName) {
    boolean match = fileUploadService.verifyMd5(dateUrl, storedName);
    SysFileUpload record = fileUploadService.findRecord(storedName);
    String storedMd5 = record != null ? record.getMd5() : null;
    return Map.of("match", match, "storedMd5", storedMd5 != null ? storedMd5 : "");
  }

  /**
   * Delete a file by its date and stored name.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).FILE_DELETE.roleCode)")
  @DeleteMapping("/{dateUrl}/{storedName}")
  public void delete(@PathVariable String dateUrl, @PathVariable String storedName) {
    fileUploadService.delete(dateUrl, storedName);
  }
}
