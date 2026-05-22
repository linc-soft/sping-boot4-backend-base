package com.lincsoft.pdf.controller;

import com.lincsoft.controller.pdf.vo.RoleListPdfRequest;
import com.lincsoft.controller.pdf.vo.UserListPdfRequest;
import com.lincsoft.pdf.dto.PdfResult;
import com.lincsoft.pdf.service.RolePdfService;
import com.lincsoft.pdf.service.UserPdfService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PDF 生成控制器。
 *
 * <p>提供 PDF 生成相关的 REST API 端点，支持角色和用户信息的列表与详情导出。
 *
 * <p>该控制器实现以下需求：
 *
 * <ul>
 *   <li>Requirement 7.1: GET /api/pdf/roles - 生成角色列表 PDF
 *   <li>Requirement 7.2: GET /api/pdf/roles/{id} - 生成角色详情 PDF
 *   <li>Requirement 7.3: GET /api/pdf/users - 生成用户列表 PDF
 *   <li>Requirement 7.4: GET /api/pdf/users/{id} - 生成用户详情 PDF
 *   <li>Requirement 7.5-7.7: PDF 响应处理及错误响应
 *   <li>Requirement 7.8-7.10: 权限验证（基于 @PreAuthorize）
 * </ul>
 *
 * <p>响应规则：
 *
 * <ul>
 *   <li>成功时返回 application/pdf 内容类型和 Content-Disposition 头
 *   <li>无数据时由 PdfExceptionHandler 处理并返回 JSON 错误
 *   <li>认证失败返回 401，权限不足返回 403，资源未找到返回 404
 * </ul>
 *
 * @author 林创科技
 * @since 2026-05-22
 */
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

  private final RolePdfService rolePdfService;
  private final UserPdfService userPdfService;

  /**
   * 生成角色列表 PDF。
   *
   * <p>根据查询参数过滤角色，并将结果导出为 PDF 文件。
   *
   * @param request 查询参数（角色名称、角色CODE）
   * @return PDF 文件流，Content-Type 为 application/pdf
   */
  @GetMapping(value = "/roles", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public ResponseEntity<byte[]> generateRoleListPdf(@Valid RoleListPdfRequest request) {
    log.info(
        "Generating role list PDF, roleName: {}, roleCode: {}",
        request.roleName(),
        request.roleCode());
    PdfResult result = rolePdfService.generateRoleListPdf(request.roleName(), request.roleCode());
    return buildPdfResponse(result);
  }

  /**
   * 生成角色详情 PDF。
   *
   * @param id 角色 ID
   * @return PDF 文件流，Content-Type 为 application/pdf
   */
  @GetMapping(value = "/roles/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ROLE_VIEW.roleCode)")
  public ResponseEntity<byte[]> generateRoleInfoPdf(@PathVariable Long id) {
    log.info("Generating role info PDF for id: {}", id);
    PdfResult result = rolePdfService.generateRoleInfoPdf(id);
    return buildPdfResponse(result);
  }

  /**
   * 生成用户列表 PDF。
   *
   * <p>根据查询参数过滤用户，并将结果导出为 PDF 文件。
   *
   * @param request 查询参数（用户名、用户状态）
   * @return PDF 文件流，Content-Type 为 application/pdf
   */
  @GetMapping(value = "/users", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER_VIEW.roleCode)")
  public ResponseEntity<byte[]> generateUserListPdf(@Valid UserListPdfRequest request) {
    log.info(
        "Generating user list PDF, username: {}, status: {}", request.username(), request.status());
    PdfResult result = userPdfService.generateUserListPdf(request.username(), request.status());
    return buildPdfResponse(result);
  }

  /**
   * 生成用户详情 PDF。
   *
   * @param id 用户 ID
   * @return PDF 文件流，Content-Type 为 application/pdf
   */
  @GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).USER_VIEW.roleCode)")
  public ResponseEntity<byte[]> generateUserInfoPdf(@PathVariable Long id) {
    log.info("Generating user info PDF for id: {}", id);
    PdfResult result = userPdfService.generateUserInfoPdf(id);
    return buildPdfResponse(result);
  }

  /**
   * 构建 PDF 响应。
   *
   * <p>设置正确的 Content-Type 和 Content-Disposition 响应头，确保浏览器以下载方式处理 PDF 文件。 使用 RFC 5987 编码处理包含非 ASCII
   * 字符（如中文、日文）的文件名。
   *
   * @param result PDF 生成结果
   * @return 包含 PDF 字节数据的响应
   */
  private ResponseEntity<byte[]> buildPdfResponse(PdfResult result) {
    String filename = result.filename();
    // 对文件名进行 URL 编码，支持中文等非 ASCII 字符
    String encodedFilename =
        URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    // 使用 RFC 5987 规范的 Content-Disposition 格式以支持非 ASCII 文件名
    headers.add(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename);
    headers.setContentLength(result.content().length);

    return ResponseEntity.ok().headers(headers).body(result.content());
  }
}
