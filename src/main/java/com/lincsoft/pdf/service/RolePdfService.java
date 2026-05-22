package com.lincsoft.pdf.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.dto.master.RoleWithParents;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstRoleInheritance;
import com.lincsoft.exception.PdfNoDataException;
import com.lincsoft.i18n.LanguageContext;
import com.lincsoft.mapper.master.MstRoleInheritanceMapper;
import com.lincsoft.mapper.master.MstRoleMapper;
import com.lincsoft.pdf.dto.PdfConfig;
import com.lincsoft.pdf.dto.PdfResult;
import com.lincsoft.pdf.dto.TableData;
import com.lincsoft.services.master.RoleService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 角色 PDF 生成服务。
 *
 * <p>负责生成角色列表 PDF 和角色详情 PDF，实现了以下需求：
 *
 * <ul>
 *   <li>Requirement 1.1: 根据请求参数查询角色列表数据
 *   <li>Requirement 1.2: 数据查询失败时阻止 PDF 生成并返回错误
 *   <li>Requirement 1.3: 数据查询无结果时阻止 PDF 生成并返回错误
 *   <li>Requirement 1.4-1.7: 角色名称和角色CODE参数校验
 *   <li>Requirement 1.8: 在文档顶部打印表头"角色列表"
 *   <li>Requirement 1.9: 打印查询条件信息
 *   <li>Requirement 1.10: 在页面右上角显示页码
 *   <li>Requirement 1.11: 打印列表表头
 *   <li>Requirement 1.12: 当角色存在继承的子角色时打印子角色列表
 *   <li>Requirement 1.13-1.16: 多语言表头和标签支持
 *   <li>Requirement 2.1: 根据角色ID查询角色详情
 *   <li>Requirement 2.5: 以"【角色名称】角色信息"格式打印表头
 *   <li>Requirement 2.6: 打印基本角色信息
 *   <li>Requirement 2.7: 当角色存在继承的父角色时打印继承角色列表
 *   <li>Requirement 2.8: 以"角色信息_[角色CODE]_[时间戳].pdf"格式生成文件名
 *   <li>Requirement 2.9-2.12: 多语言表头和标签支持
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RolePdfService {

  private final RoleService roleService;
  private final PdfGeneratorService pdfGenerator;
  private final PdfMessageService messageService;
  private final MstRoleMapper roleMapper;
  private final MstRoleInheritanceMapper roleInheritanceMapper;

  /** 时间戳格式化器 */
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  /**
   * 生成角色列表 PDF。
   *
   * <p>根据查询参数查询角色列表，生成包含角色信息的 PDF 文档。
   *
   * <p>生成流程：
   *
   * <ol>
   *   <li>查询角色数据
   *   <li>验证数据非空
   *   <li>构建 PDF 配置和表格数据
   *   <li>生成 PDF
   *   <li>返回结果
   * </ol>
   *
   * @param roleName 角色名称 (模糊匹配，1-255字符)
   * @param roleCode 角色 CODE (精确匹配，1-50字符)
   * @return PDF 结果，包含字节数组和文件名
   * @throws PdfNoDataException 如果查询结果为空
   */
  public PdfResult generateRoleListPdf(String roleName, String roleCode) {
    log.debug("Generating role list PDF with roleName: {}, roleCode: {}", roleName, roleCode);
    Locale locale = LanguageContext.getLocale();

    // 1. 查询角色数据
    List<RoleWithParents> roles = roleService.getRoleList(roleName, roleCode, null);

    // 2. 验证数据非空 (Requirement 1.3)
    if (roles.isEmpty()) {
      String errorMsg = messageService.getMessage("pdf.error.no_data", locale);
      throw new PdfNoDataException(errorMsg);
    }

    // 3. 构建 PDF 配置
    PdfConfig config = buildRoleListConfig(roleName, roleCode, locale);

    // 4. 构建表格数据（包含子角色信息）
    TableData data = buildRoleListTableData(roles, locale);

    // 5. 生成 PDF
    byte[] pdfBytes = pdfGenerator.generatePdf(config, data, locale);

    // 6. 生成文件名 (Requirement 2.8)
    String filename = generateFilename("role_list", null);

    log.debug("Role list PDF generated successfully, filename: {}", filename);
    return new PdfResult(pdfBytes, filename);
  }

  /**
   * 生成角色详情 PDF。
   *
   * <p>根据角色ID查询角色详情，生成包含角色基本信息和继承角色信息的 PDF 文档。
   *
   * <p>生成流程：
   *
   * <ol>
   *   <li>查询角色数据（包含父角色ID）
   *   <li>构建 PDF 配置
   *   <li>构建表格数据（基本属性）
   *   <li>添加继承角色信息
   *   <li>生成 PDF
   *   <li>返回结果
   * </ol>
   *
   * @param id 角色 ID
   * @return PDF 结果，包含字节数组和文件名
   * @throws com.lincsoft.exception.BusinessException 如果角色未找到
   */
  public PdfResult generateRoleInfoPdf(Long id) {
    log.debug("Generating role info PDF for id: {}", id);
    Locale locale = LanguageContext.getLocale();

    // 1. 查询角色数据（包含父角色ID）
    RoleWithParents roleWithParents = roleService.getRoleWithParentsById(id);
    MstRole role = roleWithParents.role();

    // 2. 构建 PDF 配置
    PdfConfig config = buildRoleInfoConfig(role, locale);

    // 3. 构建表格数据（基本属性 + 继承角色）
    TableData data = buildRoleInfoTableData(role, roleWithParents.parentRoleIds(), locale);

    // 4. 生成 PDF
    byte[] pdfBytes = pdfGenerator.generatePdf(config, data, locale);

    // 5. 生成文件名 (Requirement 2.8: 角色信息_[角色CODE]_[时间戳].pdf)
    String filename = generateFilename("role_info", role.getRoleCode());

    log.debug("Role info PDF generated successfully, filename: {}", filename);
    return new PdfResult(pdfBytes, filename);
  }

  // ========== 角色列表 PDF 构建方法 ==========

  /**
   * 构建角色列表 PDF 配置。
   *
   * @param roleName 角色名称查询参数
   * @param roleCode 角色 CODE 查询参数
   * @param locale 语言设置
   * @return PDF 配置
   */
  private PdfConfig buildRoleListConfig(String roleName, String roleCode, Locale locale) {
    // 构建标题
    String title = messageService.getMessage("pdf.title.role_list", locale);

    // 构建查询条件描述
    List<String> queryConditions = buildQueryConditions(roleName, roleCode, locale);

    return PdfConfig.builder().title(title).queryConditions(queryConditions).build();
  }

  /**
   * 构建查询条件描述列表。
   *
   * @param roleName 角色名称查询参数
   * @param roleCode 角色 CODE 查询参数
   * @param locale 语言设置
   * @return 查询条件描述列表
   */
  private List<String> buildQueryConditions(String roleName, String roleCode, Locale locale) {
    List<String> conditions = new ArrayList<>();

    if (roleName != null && !roleName.isBlank()) {
      String label = messageService.getMessage("pdf.header.role_name", locale);
      conditions.add(label + ": " + roleName);
    }

    if (roleCode != null && !roleCode.isBlank()) {
      String label = messageService.getMessage("pdf.header.role_code", locale);
      conditions.add(label + ": " + roleCode);
    }

    return conditions;
  }

  /**
   * 构建角色列表表格数据。
   *
   * <p>包含基本角色信息和子角色信息。
   *
   * @param roles 角色列表
   * @param locale 语言设置
   * @return 表格数据
   */
  private TableData buildRoleListTableData(List<RoleWithParents> roles, Locale locale) {
    // 构建表头
    List<String> headers = buildRoleListHeaders(locale);

    // 构建数据行和子表格
    List<List<String>> rows = new ArrayList<>();
    Map<Integer, List<List<String>>> subTables = new HashMap<>();

    // 批量获取所有角色的子角色ID
    Map<Long, List<Long>> childRoleIdsMap = batchGetChildRoleIds(roles);

    for (int i = 0; i < roles.size(); i++) {
      RoleWithParents roleWithParents = roles.get(i);
      MstRole role = roleWithParents.role();

      // 添加数据行
      List<String> row = buildRoleListRow(role);
      rows.add(row);

      // 检查是否有子角色 (Requirement 1.12)
      List<Long> childRoleIds = childRoleIdsMap.get(role.getId());
      if (childRoleIds != null && !childRoleIds.isEmpty()) {
        List<List<String>> subTable = buildChildRolesSubTable(childRoleIds, locale);
        subTables.put(i, subTable);
      }
    }

    // 设置列宽比例
    float[] columnWidths = {20f, 20f, 30f, 10f, 10f, 10f, 10f};

    return TableData.builder()
        .headers(headers)
        .rows(rows)
        .columnWidths(columnWidths)
        .subTables(subTables)
        .build();
  }

  /**
   * 构建角色列表表头。
   *
   * @param locale 语言设置
   * @return 表头列表
   */
  private List<String> buildRoleListHeaders(Locale locale) {
    List<String> headers = new ArrayList<>();
    headers.add(messageService.getMessage("pdf.header.role_name", locale));
    headers.add(messageService.getMessage("pdf.header.role_code", locale));
    headers.add(messageService.getMessage("pdf.header.description", locale));
    headers.add(messageService.getMessage("pdf.header.creator", locale));
    headers.add(messageService.getMessage("pdf.header.created_at", locale));
    headers.add(messageService.getMessage("pdf.header.updater", locale));
    headers.add(messageService.getMessage("pdf.header.updated_at", locale));
    return headers;
  }

  /**
   * 构建角色列表数据行。
   *
   * @param role 角色实体
   * @return 数据行
   */
  private List<String> buildRoleListRow(MstRole role) {
    List<String> row = new ArrayList<>();
    row.add(nullSafe(role.getRoleName()));
    row.add(nullSafe(role.getRoleCode()));
    row.add(nullSafe(role.getDescription()));
    row.add(nullSafe(role.getCreateBy()));
    row.add(formatDateTime(role.getCreateAt()));
    row.add(nullSafe(role.getUpdateBy()));
    row.add(formatDateTime(role.getUpdateAt()));
    return row;
  }

  /**
   * 批量获取所有角色的子角色ID。
   *
   * <p>使用批量查询避免 N+1 问题。
   *
   * @param roles 角色列表
   * @return 角色 ID 到子角色 ID 列表的映射
   */
  private Map<Long, List<Long>> batchGetChildRoleIds(List<RoleWithParents> roles) {
    if (roles == null || roles.isEmpty()) {
      return Map.of();
    }

    // 提取所有角色 ID
    List<Long> roleIds = roles.stream().map(r -> r.role().getId()).toList();

    // 批量查询继承关系：parent_role_id -> child_role_id
    QueryWrapper<MstRoleInheritance> qw = new QueryWrapper<>();
    qw.in("parent_role_id", roleIds);

    Map<Long, List<Long>> childRoleIdsMap = new HashMap<>();
    roleInheritanceMapper
        .selectList(qw)
        .forEach(
            ri ->
                childRoleIdsMap
                    .computeIfAbsent(ri.getParentRoleId(), k -> new ArrayList<>())
                    .add(ri.getChildRoleId()));

    return childRoleIdsMap;
  }

  /**
   * 构建子角色子表格数据。
   *
   * <p>子表格包含子角色的角色名称、角色CODE和说明。
   *
   * @param childRoleIds 子角色 ID 列表
   * @param locale 语言设置
   * @return 子表格数据（第一行为表头，其余为数据行）
   */
  private List<List<String>> buildChildRolesSubTable(List<Long> childRoleIds, Locale locale) {
    List<List<String>> subTable = new ArrayList<>();

    // 子表格表头
    List<String> subHeaders = new ArrayList<>();
    subHeaders.add(messageService.getMessage("pdf.label.inherited_roles", locale));
    subHeaders.add(messageService.getMessage("pdf.header.role_code", locale));
    subHeaders.add(messageService.getMessage("pdf.header.description", locale));
    subTable.add(subHeaders);

    // 查询子角色详情
    List<MstRole> childRoles = roleMapper.selectBatchIds(childRoleIds);
    for (MstRole childRole : childRoles) {
      List<String> row = new ArrayList<>();
      row.add(nullSafe(childRole.getRoleName()));
      row.add(nullSafe(childRole.getRoleCode()));
      row.add(nullSafe(childRole.getDescription()));
      subTable.add(row);
    }

    return subTable;
  }

  // ========== 角色详情 PDF 构建方法 ==========

  /**
   * 构建角色详情 PDF 配置。
   *
   * @param role 角色实体
   * @param locale 语言设置
   * @return PDF 配置
   */
  private PdfConfig buildRoleInfoConfig(MstRole role, Locale locale) {
    // 构建标题 (Requirement 2.5: 【角色名称】角色信息)
    String titleTemplate = messageService.getMessage("pdf.title.role_info", locale);
    String title = titleTemplate.replace("{0}", role.getRoleName());

    return PdfConfig.builder().title(title).queryConditions(null).build();
  }

  /**
   * 构建角色详情表格数据。
   *
   * <p>包含基本属性和继承角色信息。
   *
   * @param role 角色实体
   * @param parentRoleIds 父角色 ID 列表
   * @param locale 语言设置
   * @return 表格数据
   */
  private TableData buildRoleInfoTableData(MstRole role, List<Long> parentRoleIds, Locale locale) {
    // 构建表头
    List<String> headers = new ArrayList<>();
    headers.add(messageService.getMessage("pdf.header.property", locale));
    headers.add(messageService.getMessage("pdf.header.value", locale));

    // 构建数据行
    List<List<String>> rows = buildRoleInfoRows(role, parentRoleIds, locale);

    // 设置列宽比例
    float[] columnWidths = {30f, 70f};

    return TableData.builder().headers(headers).rows(rows).columnWidths(columnWidths).build();
  }

  /**
   * 构建角色详情数据行。
   *
   * <p>包含角色基本属性和继承角色信息。
   *
   * @param role 角色实体
   * @param parentRoleIds 父角色 ID 列表
   * @param locale 语言设置
   * @return 数据行列表
   */
  private List<List<String>> buildRoleInfoRows(
      MstRole role, List<Long> parentRoleIds, Locale locale) {
    List<List<String>> rows = new ArrayList<>();

    // 角色ID
    addPropertyRow(rows, "pdf.header.role_id", String.valueOf(role.getId()), locale);

    // 角色名称
    addPropertyRow(rows, "pdf.header.role_name", role.getRoleName(), locale);

    // 角色CODE
    addPropertyRow(rows, "pdf.header.role_code", role.getRoleCode(), locale);

    // 说明
    addPropertyRow(rows, "pdf.header.description", role.getDescription(), locale);

    // 创建者
    addPropertyRow(rows, "pdf.header.creator", role.getCreateBy(), locale);

    // 创建时间
    addPropertyRow(rows, "pdf.header.created_at", formatDateTime(role.getCreateAt()), locale);

    // 更新者
    addPropertyRow(rows, "pdf.header.updater", role.getUpdateBy(), locale);

    // 更新时间
    addPropertyRow(rows, "pdf.header.updated_at", formatDateTime(role.getUpdateAt()), locale);

    // 继承角色 (Requirement 2.7)
    String inheritedRolesLabel = messageService.getMessage("pdf.label.inherited_roles", locale);
    if (parentRoleIds != null && !parentRoleIds.isEmpty()) {
      // 查询父角色详情
      List<MstRole> parentRoles = roleMapper.selectBatchIds(parentRoleIds);
      String parentRolesStr =
          parentRoles.stream()
              .map(r -> r.getRoleName() + " (" + nullSafe(r.getRoleCode()) + ")")
              .reduce((a, b) -> a + ", " + b)
              .orElse("");
      addPropertyRow(rows, inheritedRolesLabel, parentRolesStr, locale);
    } else {
      // 无继承角色
      String noInheritedRoles = messageService.getMessage("pdf.label.no_inherited_roles", locale);
      addPropertyRow(rows, inheritedRolesLabel, noInheritedRoles, locale);
    }

    return rows;
  }

  /**
   * 添加属性行到数据行列表。
   *
   * @param rows 数据行列表
   * @param labelKey 标签消息键
   * @param value 属性值
   * @param locale 语言设置
   */
  private void addPropertyRow(
      List<List<String>> rows, String labelKey, String value, Locale locale) {
    List<String> row = new ArrayList<>();
    row.add(messageService.getMessage(labelKey, locale));
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
   *   <li>角色列表：role_list_[时间戳].pdf
   *   <li>角色详情：角色信息_[角色CODE]_[时间戳].pdf
   * </ul>
   *
   * @param type 文件类型 ("role_list" 或 "role_info")
   * @param roleCode 角色 CODE（仅角色详情时使用）
   * @return 文件名
   */
  private String generateFilename(String type, String roleCode) {
    String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    Locale locale = LanguageContext.getLocale();

    if ("role_info".equals(type) && roleCode != null && !roleCode.isBlank()) {
      // Requirement 2.8: 角色信息_[角色CODE]_[时间戳].pdf
      String baseName = messageService.getMessage("pdf.filename.role_info", locale);
      return baseName + "_" + roleCode + "_" + timestamp + ".pdf";
    } else {
      // 角色列表
      String baseName = messageService.getMessage("pdf.filename.role_list", locale);
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
  private String formatDateTime(java.time.LocalDateTime dateTime) {
    return dateTime != null
        ? dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        : "";
  }
}
