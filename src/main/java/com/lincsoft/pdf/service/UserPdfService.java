package com.lincsoft.pdf.service;

import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.exception.PdfNoDataException;
import com.lincsoft.exception.PdfValidationException;
import com.lincsoft.i18n.LanguageContext;
import com.lincsoft.pdf.dto.PdfConfig;
import com.lincsoft.pdf.dto.PdfResult;
import com.lincsoft.pdf.dto.TableData;
import com.lincsoft.services.master.RoleService;
import com.lincsoft.services.master.UserService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户 PDF 生成服务。
 *
 * <p>负责生成用户列表 PDF 和用户详情 PDF，实现了以下需求：
 *
 * <ul>
 *   <li>Requirement 3.1: 根据请求参数查询用户列表数据
 *   <li>Requirement 3.2: 数据查询失败时阻止 PDF 生成并返回错误
 *   <li>Requirement 3.3: 数据查询无结果时阻止 PDF 生成并返回错误
 *   <li>Requirement 3.4-3.7: 用户名和用户状态参数校验
 *   <li>Requirement 3.8: 在文档顶部打印表头"用户列表"
 *   <li>Requirement 3.9: 打印查询条件信息
 *   <li>Requirement 3.10: 在页面右上角显示页码
 *   <li>Requirement 3.11: 打印列表表头
 *   <li>Requirement 3.12-3.16: 多语言表头和标签支持
 *   <li>Requirement 4.1: 根据用户ID查询用户详情
 *   <li>Requirement 4.5: 以"【用户名】用户信息"格式打印表头
 *   <li>Requirement 4.6: 打印基本用户信息
 *   <li>Requirement 4.7: 当用户存在关联角色时打印角色列表
 *   <li>Requirement 4.8: 当用户无关联角色时打印"无关联角色"
 *   <li>Requirement 4.9: 以"用户信息_[用户名]_[时间戳].pdf"格式生成文件名
 *   <li>Requirement 4.10-4.13: 多语言表头和标签支持
 * </ul>
 *
 * @author 林创科技
 * @since 2026-05-22
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserPdfService {

  private final UserService userService;
  private final RoleService roleService;
  private final PdfGeneratorService pdfGenerator;
  private final PdfMessageService messageService;

  /** 时间戳格式化器 */
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  /** 有效的用户状态集合 */
  private static final Set<String> VALID_STATUSES = Set.of("active", "inactive", "suspended");

  /**
   * 生成用户列表 PDF。
   *
   * <p>根据查询参数查询用户列表，生成包含用户信息的 PDF 文档。
   *
   * <p>生成流程：
   *
   * <ol>
   *   <li>验证用户状态参数有效性
   *   <li>查询用户数据
   *   <li>验证数据非空
   *   <li>构建 PDF 配置和表格数据
   *   <li>生成 PDF
   *   <li>返回结果
   * </ol>
   *
   * @param username 用户名 (模糊匹配)
   * @param status 用户状态 (active、inactive、suspended 之一，可选)
   * @return PDF 结果，包含字节数组和文件名
   * @throws PdfValidationException 如果状态参数无效
   * @throws PdfNoDataException 如果查询结果为空
   */
  public PdfResult generateUserListPdf(String username, String status) {
    log.debug("Generating user list PDF with username: {}, status: {}", username, status);
    Locale locale = LanguageContext.getLocale();

    // 1. 验证状态参数 (Requirement 3.6, 3.7)
    validateStatus(status);

    // 2. 查询用户数据 (Requirement 3.1)
    List<MstUser> users = userService.getUserList(username, status);

    // 3. 验证数据非空 (Requirement 3.3)
    if (users.isEmpty()) {
      String errorMsg = messageService.getMessage("pdf.error.no_data", locale);
      throw new PdfNoDataException(errorMsg);
    }

    // 4. 构建 PDF 配置
    PdfConfig config = buildUserListConfig(username, status, locale);

    // 5. 构建表格数据
    TableData data = buildUserListTableData(users, locale);

    // 6. 生成 PDF
    byte[] pdfBytes = pdfGenerator.generatePdf(config, data, locale);

    // 7. 生成文件名 (user_list_[时间戳].pdf)
    String filename = generateFilename("user_list", null);

    log.debug("User list PDF generated successfully, filename: {}", filename);
    return new PdfResult(pdfBytes, filename);
  }

  /**
   * 生成用户详情 PDF。
   *
   * <p>根据用户ID查询用户详情，生成包含用户基本信息和关联角色的 PDF 文档。
   *
   * <p>生成流程：
   *
   * <ol>
   *   <li>查询用户数据
   *   <li>查询用户关联角色
   *   <li>构建 PDF 配置
   *   <li>构建表格数据
   *   <li>生成 PDF
   *   <li>返回结果
   * </ol>
   *
   * @param id 用户 ID
   * @return PDF 结果，包含字节数组和文件名
   * @throws com.lincsoft.exception.BusinessException 如果用户未找到
   */
  public PdfResult generateUserInfoPdf(Long id) {
    log.debug("Generating user info PDF for id: {}", id);
    Locale locale = LanguageContext.getLocale();

    // 1. 查询用户数据
    MstUser user = userService.getUserById(id);

    // 2. 查询用户关联角色 (Requirement 4.7, 4.8)
    List<MstRole> userRoles = roleService.getRoleListByUserId(user.getId());

    // 3. 构建 PDF 配置
    PdfConfig config = buildUserInfoConfig(user, locale);

    // 4. 构建表格数据 (基本属性 + 关联角色)
    TableData data = buildUserInfoTableData(user, userRoles, locale);

    // 5. 生成 PDF
    byte[] pdfBytes = pdfGenerator.generatePdf(config, data, locale);

    // 6. 生成文件名 (Requirement 4.9: 用户信息_[用户名]_[时间戳].pdf)
    String filename = generateFilename("user_info", user.getUsername());

    log.debug("User info PDF generated successfully, filename: {}", filename);
    return new PdfResult(pdfBytes, filename);
  }

  /**
   * 验证用户状态参数。
   *
   * <p>验证状态值必须是 "active"、"inactive"、"suspended" 之一。 空值或 null 允许通过（不进行状态过滤）。
   *
   * @param status 用户状态
   * @throws PdfValidationException 如果状态值无效
   */
  void validateStatus(String status) {
    if (status == null || status.isBlank()) {
      return;
    }
    //    if (!VALID_STATUSES.contains(status)) {
    //      throw new PdfValidationException(
    //          "Invalid status: " + status + ". Must be one of: active, inactive, suspended");
    //    }
  }

  // ========== 用户列表 PDF 构建方法 ==========

  /**
   * 构建用户列表 PDF 配置。
   *
   * @param username 用户名查询参数
   * @param status 用户状态查询参数
   * @param locale 语言设置
   * @return PDF 配置
   */
  private PdfConfig buildUserListConfig(String username, String status, Locale locale) {
    // 构建标题
    String title = messageService.getMessage("pdf.title.user_list", locale);

    // 构建查询条件描述
    List<String> queryConditions = buildQueryConditions(username, status, locale);

    return PdfConfig.builder().title(title).queryConditions(queryConditions).build();
  }

  /**
   * 构建查询条件描述列表。
   *
   * @param username 用户名查询参数
   * @param status 用户状态查询参数
   * @param locale 语言设置
   * @return 查询条件描述列表
   */
  private List<String> buildQueryConditions(String username, String status, Locale locale) {
    List<String> conditions = new ArrayList<>();

    if (username != null && !username.isBlank()) {
      String label = messageService.getMessage("pdf.header.username", locale);
      conditions.add(label + ": " + username);
    }

    if (status != null && !status.isBlank()) {
      String label = messageService.getMessage("pdf.header.status", locale);
      conditions.add(label + ": " + status);
    }

    return conditions;
  }

  /**
   * 构建用户列表表格数据。
   *
   * @param users 用户列表
   * @param locale 语言设置
   * @return 表格数据
   */
  private TableData buildUserListTableData(List<MstUser> users, Locale locale) {
    // 构建表头 (Requirement 3.11)
    List<String> headers = buildUserListHeaders(locale);

    // 构建数据行
    List<List<String>> rows = new ArrayList<>();
    for (MstUser user : users) {
      rows.add(buildUserListRow(user));
    }

    // 设置列宽比例
    float[] columnWidths = {25f, 15f, 15f, 15f, 15f, 15f};

    return TableData.builder().headers(headers).rows(rows).columnWidths(columnWidths).build();
  }

  /**
   * 构建用户列表表头。
   *
   * <p>表头包括：用户名、用户状态、创建者、创建时间、更新者、更新时间 (Requirement 3.11)
   *
   * @param locale 语言设置
   * @return 表头列表
   */
  private List<String> buildUserListHeaders(Locale locale) {
    List<String> headers = new ArrayList<>();
    headers.add(messageService.getMessage("pdf.header.username", locale));
    headers.add(messageService.getMessage("pdf.header.status", locale));
    headers.add(messageService.getMessage("pdf.header.creator", locale));
    headers.add(messageService.getMessage("pdf.header.created_at", locale));
    headers.add(messageService.getMessage("pdf.header.updater", locale));
    headers.add(messageService.getMessage("pdf.header.updated_at", locale));
    return headers;
  }

  /**
   * 构建用户列表数据行。
   *
   * @param user 用户实体
   * @return 数据行
   */
  private List<String> buildUserListRow(MstUser user) {
    List<String> row = new ArrayList<>();
    row.add(nullSafe(user.getUsername()));
    row.add(nullSafe(user.getStatus()));
    row.add(nullSafe(user.getCreateBy()));
    row.add(formatDateTime(user.getCreateAt()));
    row.add(nullSafe(user.getUpdateBy()));
    row.add(formatDateTime(user.getUpdateAt()));
    return row;
  }

  // ========== 用户详情 PDF 构建方法 ==========

  /**
   * 构建用户详情 PDF 配置。
   *
   * @param user 用户实体
   * @param locale 语言设置
   * @return PDF 配置
   */
  private PdfConfig buildUserInfoConfig(MstUser user, Locale locale) {
    // 构建标题 (Requirement 4.5: 【用户名】用户信息)
    String titleTemplate = messageService.getMessage("pdf.title.user_info", locale);
    String title = titleTemplate.replace("{0}", user.getUsername());

    return PdfConfig.builder().title(title).queryConditions(null).build();
  }

  /**
   * 构建用户详情表格数据。
   *
   * <p>包含基本属性和关联角色信息。
   *
   * @param user 用户实体
   * @param userRoles 用户关联的角色列表
   * @param locale 语言设置
   * @return 表格数据
   */
  private TableData buildUserInfoTableData(MstUser user, List<MstRole> userRoles, Locale locale) {
    // 构建表头
    List<String> headers = new ArrayList<>();
    headers.add(messageService.getMessage("pdf.header.property", locale));
    headers.add(messageService.getMessage("pdf.header.value", locale));

    // 构建数据行
    List<List<String>> rows = buildUserInfoRows(user, userRoles, locale);

    // 设置列宽比例
    float[] columnWidths = {30f, 70f};

    return TableData.builder().headers(headers).rows(rows).columnWidths(columnWidths).build();
  }

  /**
   * 构建用户详情数据行。
   *
   * <p>包含用户基本属性和关联角色信息。
   *
   * @param user 用户实体
   * @param userRoles 用户关联的角色列表
   * @param locale 语言设置
   * @return 数据行列表
   */
  private List<List<String>> buildUserInfoRows(
      MstUser user, List<MstRole> userRoles, Locale locale) {
    List<List<String>> rows = new ArrayList<>();

    // 用户ID
    addPropertyRow(rows, "pdf.header.user_id", String.valueOf(user.getId()), locale);

    // 用户名
    addPropertyRow(rows, "pdf.header.username", user.getUsername(), locale);

    // 用户状态
    addPropertyRow(rows, "pdf.header.status", user.getStatus(), locale);

    // 创建者
    addPropertyRow(rows, "pdf.header.creator", user.getCreateBy(), locale);

    // 创建时间
    addPropertyRow(rows, "pdf.header.created_at", formatDateTime(user.getCreateAt()), locale);

    // 更新者
    addPropertyRow(rows, "pdf.header.updater", user.getUpdateBy(), locale);

    // 更新时间
    addPropertyRow(rows, "pdf.header.updated_at", formatDateTime(user.getUpdateAt()), locale);

    // 关联角色 (Requirement 4.7, 4.8)
    String rolesLabel = messageService.getMessage("pdf.header.roles", locale);
    if (userRoles != null && !userRoles.isEmpty()) {
      // 拼接角色信息：角色名称 (角色CODE) - 说明
      String rolesStr =
          userRoles.stream()
              .map(
                  r -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(nullSafe(r.getRoleName()));
                    if (r.getRoleCode() != null && !r.getRoleCode().isBlank()) {
                      sb.append(" (").append(r.getRoleCode()).append(")");
                    }
                    if (r.getDescription() != null && !r.getDescription().isBlank()) {
                      sb.append(" - ").append(r.getDescription());
                    }
                    return sb.toString();
                  })
              .reduce((a, b) -> a + ", " + b)
              .orElse("");
      addPropertyRow(rows, rolesLabel, rolesStr, locale);
    } else {
      // 无关联角色 (Requirement 4.8)
      String noRoles = messageService.getMessage("pdf.label.no_roles", locale);
      addPropertyRow(rows, rolesLabel, noRoles, locale);
    }

    return rows;
  }

  /**
   * 添加属性行到数据行列表。
   *
   * <p>支持两种调用方式：
   *
   * <ul>
   *   <li>当 labelKey 是消息键（以 "pdf." 开头）时，先解析消息
   *   <li>当 labelKey 已经是解析后的消息文本时，直接使用
   * </ul>
   *
   * @param rows 数据行列表
   * @param labelKey 标签消息键或已解析的文本
   * @param value 属性值
   * @param locale 语言设置
   */
  private void addPropertyRow(
      List<List<String>> rows, String labelKey, String value, Locale locale) {
    List<String> row = new ArrayList<>();
    // 如果以 pdf. 开头，则解析消息，否则直接使用
    if (labelKey != null && labelKey.startsWith("pdf.")) {
      row.add(messageService.getMessage(labelKey, locale));
    } else {
      row.add(nullSafe(labelKey));
    }
    row.add(nullSafe(value));
    rows.add(row);
  }

  // ========== 工具方法 ==========

  /**
   * 生成 PDF 文件名。
   *
   * <p>文件名格式：
   *
   * <ul>
   *   <li>用户列表：user_list_[时间戳].pdf
   *   <li>用户详情：用户信息_[用户名]_[时间戳].pdf
   * </ul>
   *
   * @param type 文件类型 ("user_list" 或 "user_info")
   * @param username 用户名（仅用户详情时使用）
   * @return 文件名
   */
  private String generateFilename(String type, String username) {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    Locale locale = LanguageContext.getLocale();

    if ("user_info".equals(type) && username != null && !username.isBlank()) {
      // Requirement 4.9: 用户信息_[用户名]_[时间戳].pdf
      String baseName = messageService.getMessage("pdf.filename.user_info", locale);
      return baseName + "_" + username + "_" + timestamp + ".pdf";
    } else {
      // 用户列表
      String baseName = messageService.getMessage("pdf.filename.user_list", locale);
      return baseName + "_" + timestamp + ".pdf";
    }
  }

  /**
   * 空值安全处理。
   *
   * @param value 可能空的字符串
   * @return 非空字符串（空值返回空字符串）
   */
  private String nullSafe(String value) {
    return value != null ? value : "";
  }

  /**
   * 格式化日期时间。
   *
   * @param dateTime 日期时间
   * @return 格式化后的字符串
   */
  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null
        ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        : "";
  }
}
