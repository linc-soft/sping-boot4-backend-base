package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.entity.system.SysFileUpload;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysFileUploadMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * File upload service.
 *
 * <p>Handles file storage operations including upload, download path resolution, and deletion.
 * Files are organized by date-based subdirectories (yyyy/MM/dd) under the configured upload
 * directory. Each uploaded file is assigned a UUID-based storage name to prevent conflicts. Upload
 * metadata is persisted to the {@code sys_file_upload} table for audit and retrieval.
 *
 * <p>MD5 hash is computed during upload and stored in the database. The hash can be verified later
 * against the file on disk to detect tampering or corruption.
 *
 * @author 林创科技
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

  private final AppProperties appProperties;

  private final SysFileUploadMapper sysFileUploadMapper;

  private static final DateTimeFormatter DATE_PATH_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private static final DateTimeFormatter DATE_URL_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  /** File type constants */
  public static final int FILE_TYPE_IMAGE = 1;

  public static final int FILE_TYPE_DOCUMENT = 2;
  public static final int FILE_TYPE_ARCHIVE = 3;
  public static final int FILE_TYPE_OTHER = 9;

  private static final Map<Integer, Set<String>> FILE_TYPE_EXTENSIONS =
      Map.of(
          FILE_TYPE_IMAGE, Set.of("jpg", "jpeg", "png", "gif", "bmp", "svg"),
          FILE_TYPE_DOCUMENT,
              Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv"),
          FILE_TYPE_ARCHIVE, Set.of("zip", "rar", "7z"));

  /**
   * Upload a single file to the local storage and persist metadata to the database.
   *
   * <p>The file is stored under {@code <upload-dir>/<yyyy/MM/dd>/<uuid>.<ext>} with its original
   * filename preserved in the returned metadata. MD5 hash is computed during the write operation.
   *
   * @param file the multipart file uploaded by the client
   * @param associateType optional association type (e.g., "USER_AVATAR", "TICKET_ATTACHMENT")
   * @param associateId optional associated business entity ID
   * @return metadata describing the stored file, including the database record ID and MD5 hash
   * @throws BusinessException if the file is empty, too large, has a disallowed extension, or
   *     upload fails
   */
  public FileMetadata upload(MultipartFile file, String associateType, Long associateId) {
    validateFile(file);

    String originalFilename = file.getOriginalFilename();
    String extension = extractExtension(originalFilename);
    validateExtension(extension);

    String storedName = UUID.randomUUID().toString() + "." + extension;
    LocalDate today = LocalDate.now();
    String datePath = today.format(DATE_PATH_FORMATTER);
    String dateUrl = today.format(DATE_URL_FORMATTER);
    Path targetDir = Paths.get(appProperties.getUpload().getDirectory(), datePath);
    long fileSize = file.getSize();
    String contentType = file.getContentType();

    // Write file to disk and compute MD5 hash in a single pass
    String md5;
    try {
      Files.createDirectories(targetDir);
      Path targetFile = targetDir.resolve(storedName);

      MessageDigest digest = MessageDigest.getInstance("MD5");
      try (InputStream raw = file.getInputStream();
          DigestInputStream dis = new DigestInputStream(raw, digest)) {
        Files.copy(dis, targetFile, StandardCopyOption.REPLACE_EXISTING);
      }
      md5 = HexFormat.of().formatHex(digest.digest());
      log.info("File stored: {} -> {} (MD5: {})", originalFilename, targetFile, md5);
    } catch (IOException | NoSuchAlgorithmException e) {
      log.error("Failed to store file: {}", originalFilename, e);
      throw new BusinessException(MessageEnums.SYS_FILE_UPLOAD_FAILED);
    }

    // Persist metadata to database
    String currentUser = getCurrentUser();
    LocalDateTime now = LocalDateTime.now();
    String filePath = datePath + "/" + storedName;

    SysFileUpload record = new SysFileUpload();
    record.setStoredName(storedName);
    record.setOriginalFilename(originalFilename);
    record.setExtension(extension);
    record.setFileType(detectFileType(extension));
    record.setFileSize(fileSize);
    record.setContentType(contentType);
    record.setFilePath(filePath);
    record.setDatePath(datePath);
    record.setDateUrl(dateUrl);
    record.setAssociateType(associateType);
    record.setAssociateId(associateId);
    record.setMd5(md5);
    record.setCreator(currentUser);
    record.setCreateTime(now);

    sysFileUploadMapper.insert(record);
    log.info("File record saved: id={}, storedName={}", record.getId(), storedName);

    return new FileMetadata(
        record.getId(),
        storedName,
        originalFilename,
        datePath,
        dateUrl,
        fileSize,
        contentType,
        md5);
  }

  /**
   * Upload a single file without association.
   *
   * @param file the multipart file uploaded by the client
   * @return metadata describing the stored file
   */
  public FileMetadata upload(MultipartFile file) {
    return upload(file, null, null);
  }

  /** Validate a multipart file before upload. */
  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(MessageEnums.SYS_FILE_EMPTY);
    }
    long maxBytes = appProperties.getUpload().getMaxFileSizeMb() * 1024 * 1024;
    if (file.getSize() > maxBytes) {
      throw new BusinessException(MessageEnums.SYS_FILE_TOO_LARGE);
    }
  }

  /** Validate the file extension against the allowed list. */
  private void validateExtension(String extension) {
    List<String> allowed = appProperties.getUpload().getAllowedExtensions();
    if (allowed != null && !allowed.isEmpty()) {
      if (extension == null || !allowed.contains(extension.toLowerCase())) {
        throw new BusinessException(MessageEnums.SYS_FILE_EXTENSION_NOT_ALLOWED);
      }
    }
  }

  /** Extract the file extension from a filename. */
  private String extractExtension(String filename) {
    if (filename == null || !filename.contains(".")) {
      return "bin";
    }
    return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
  }

  /**
   * Detect file type category from its extension.
   *
   * @param extension the file extension (lowercase, without dot)
   * @return file type code (1=image, 2=document, 3=archive), or null if unrecognized
   */
  private Integer detectFileType(String extension) {
    if (extension == null) {
      return FILE_TYPE_OTHER;
    }
    return FILE_TYPE_EXTENSIONS.entrySet().stream()
        .filter(e -> e.getValue().contains(extension))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(FILE_TYPE_OTHER);
  }

  /**
   * Resolve the full path of a stored file.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   * @return the full path to the stored file
   * @throws BusinessException if the file does not exist
   */
  public Path resolveFilePath(String dateUrl, String storedName) {
    String datePath = dateUrl.replace("-", "/");
    Path filePath =
        Paths.get(appProperties.getUpload().getDirectory(), datePath, storedName).normalize();
    if (!Files.exists(filePath)) {
      throw new BusinessException(MessageEnums.SYS_FILE_NOT_FOUND);
    }
    return filePath;
  }

  /**
   * Look up the database record for a stored file.
   *
   * @param storedName the UUID-based storage filename
   * @return the database record, or null if not found
   */
  public SysFileUpload findRecord(String storedName) {
    return sysFileUploadMapper.selectOne(
        new LambdaQueryWrapper<SysFileUpload>().eq(SysFileUpload::getStoredName, storedName));
  }

  /**
   * Verify that the file on disk matches the stored MD5 hash in the database.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   * @return true if the MD5 matches, false otherwise
   */
  public boolean verifyMd5(String dateUrl, String storedName) {
    Path filePath = resolveFilePath(dateUrl, storedName);
    SysFileUpload record = findRecord(storedName);
    if (record == null || record.getMd5() == null) {
      return false;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      try (InputStream is = Files.newInputStream(filePath);
          DigestInputStream dis = new DigestInputStream(is, digest)) {
        dis.transferTo(OutputStream.nullOutputStream());
      }
      String currentMd5 = HexFormat.of().formatHex(digest.digest());
      return currentMd5.equalsIgnoreCase(record.getMd5());
    } catch (IOException | NoSuchAlgorithmException e) {
      log.error("MD5 verification failed: {}", filePath, e);
      return false;
    }
  }

  /**
   * Delete a stored file and its database record.
   *
   * @param dateUrl the date in URL format (e.g., "2026-06-03")
   * @param storedName the UUID-based storage filename
   * @throws BusinessException if the file does not exist or deletion fails
   */
  public void delete(String dateUrl, String storedName) {
    String datePath = dateUrl.replace("-", "/");
    Path filePath =
        Paths.get(appProperties.getUpload().getDirectory(), datePath, storedName).normalize();
    if (!Files.exists(filePath)) {
      throw new BusinessException(MessageEnums.SYS_FILE_NOT_FOUND);
    }
    try {
      Files.delete(filePath);
      log.info("File deleted from disk: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to delete file from disk: {}", filePath, e);
      throw new BusinessException(MessageEnums.SYS_FILE_UPLOAD_FAILED);
    }

    // Delete database record (logical delete via @TableLogic)
    SysFileUpload record = findRecord(storedName);
    if (record != null) {
      sysFileUploadMapper.deleteById(record.getId());
      log.info("File record deleted: id={}", record.getId());
    }
  }

  /** Get the current username from MDC. */
  private String getCurrentUser() {
    String username = MDC.get(CommonConstants.MDC_CURRENT_USER_KEY);
    return (username != null && !username.isBlank())
        ? username
        : CommonConstants.MDC_DEFAULT_USERNAME;
  }

  /**
   * Metadata of an uploaded file.
   *
   * @param id database record ID (null if not persisted)
   * @param storedName UUID-based storage filename
   * @param originalFilename original filename from the client
   * @param datePath date-based subdirectory (e.g., "2026/06/03")
   * @param dateUrl date in URL-friendly format (e.g., "2026-06-03")
   * @param size file size in bytes
   * @param contentType MIME type of the file
   * @param md5 MD5 hash of the file content
   */
  public record FileMetadata(
      Long id,
      String storedName,
      String originalFilename,
      String datePath,
      String dateUrl,
      long size,
      String contentType,
      String md5) {}
}
