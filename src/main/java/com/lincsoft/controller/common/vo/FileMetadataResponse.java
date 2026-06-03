package com.lincsoft.controller.common.vo;

/**
 * File metadata response VO.
 *
 * @param id database record ID
 * @param storedName UUID-based storage filename
 * @param originalFilename original filename from the client
 * @param fileType file type code
 * @param datePath date-based subdirectory (e.g., "2026/06/03")
 * @param dateUrl date in URL-friendly format (e.g., "2026-06-03")
 * @param size file size in bytes
 * @param contentType MIME type of the file
 * @param md5 MD5 hash of the file content
 */
public record FileMetadataResponse(
    Long id,
    String storedName,
    String originalFilename,
    Integer fileType,
    String datePath,
    String dateUrl,
    Long size,
    String contentType,
    String md5) {}
