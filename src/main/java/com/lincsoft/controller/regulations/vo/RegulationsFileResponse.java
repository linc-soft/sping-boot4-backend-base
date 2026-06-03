package com.lincsoft.controller.regulations.vo;

import java.time.LocalDateTime;

/**
 * Regulations file response VO.
 *
 * @param id File upload ID
 * @param originalFilename Original filename
 * @param fileType File type code
 * @param dateUrl Date-based URL path segment
 * @param storedName Stored filename
 * @param contentType MIME type
 * @param size File size in bytes
 * @param uploader Uploader username
 * @param uploadDate Upload date
 * @author lincsoft
 * @since 2026-06-03
 */
public record RegulationsFileResponse(
    Long id,
    String originalFilename,
    Integer fileType,
    String dateUrl,
    String storedName,
    String contentType,
    Long size,
    String uploader,
    LocalDateTime uploadDate) {}
