package com.lincsoft.controller.regulations;

import com.lincsoft.controller.common.vo.FileMetadataResponse;
import com.lincsoft.controller.regulations.vo.RegulationsFileResponse;
import com.lincsoft.services.regulations.RegulationsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Regulations controller.
 *
 * <p>Manages regulation document uploads and listings.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/regulations")
@RequiredArgsConstructor
public class RegulationsController {

  private final RegulationsService regulationsService;

  /**
   * Upload a regulations file.
   *
   * @param file the file to upload
   * @return file metadata
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).REGULATIONS_WRITE.roleCode)")
  @PostMapping("/upload")
  public FileMetadataResponse uploadRegulations(@RequestParam("file") MultipartFile file) {
    return regulationsService.uploadRegulations(file);
  }

  /**
   * List all regulations files.
   *
   * @return list of regulations file responses
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  @GetMapping("/list")
  public List<RegulationsFileResponse> listRegulations() {
    return regulationsService.listRegulations();
  }
}
