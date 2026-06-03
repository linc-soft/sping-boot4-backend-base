package com.lincsoft.services.regulations;

import com.lincsoft.constant.FileAssociateType;
import com.lincsoft.controller.common.vo.FileMetadataResponse;
import com.lincsoft.controller.regulations.vo.RegulationsFileResponse;
import com.lincsoft.entity.system.SysFileUpload;
import com.lincsoft.services.system.FileUploadService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Regulations service for managing regulation documents.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegulationsService {

  private final FileUploadService fileUploadService;

  /**
   * Upload a regulations file.
   *
   * @param file the multipart file
   * @return file metadata
   */
  public FileMetadataResponse uploadRegulations(MultipartFile file) {
    return fileUploadService.upload(file, FileAssociateType.REGULATIONS, null);
  }

  /**
   * List all regulations files.
   *
   * @return list of regulations file responses
   */
  public List<RegulationsFileResponse> listRegulations() {
    List<SysFileUpload> files =
        fileUploadService.findByAssociate(FileAssociateType.REGULATIONS, null);

    return files.stream()
        .map(
            f ->
                new RegulationsFileResponse(
                    f.getId(),
                    f.getOriginalFilename(),
                    f.getFileType(),
                    f.getDateUrl(),
                    f.getStoredName(),
                    f.getContentType(),
                    f.getFileSize(),
                    f.getCreator(),
                    f.getCreateTime()))
        .collect(Collectors.toList());
  }
}
