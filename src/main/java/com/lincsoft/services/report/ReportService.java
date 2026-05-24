package com.lincsoft.services.report;

import com.lincsoft.config.AppProperties;
import com.lincsoft.controller.master.vo.UserListReportRequest;
import com.lincsoft.dto.master.UserListGroupDto;
import com.lincsoft.dto.master.UserReportItem;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.services.master.RoleService;
import com.lincsoft.services.master.UserService;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Report generation service.
 *
 * <p>Prepares report data and delegates PDF rendering to {@link PdfGeneratorService}. Handles data
 * querying, grouping logic, and template variable assembly.
 *
 * @author 林创科技
 * @since 2026-05-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

  private final PdfGeneratorService pdfGeneratorService;
  private final AppProperties appProperties;
  private final UserService userService;
  private final RoleService roleService;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /**
   * Generates a user list PDF report.
   *
   * <p>Queries user data based on the username filter, optionally groups the data by role or base
   * role, and renders a PDF report using the Thymeleaf template.
   *
   * <p>Grouping behavior:
   *
   * <ul>
   *   <li>{@code role} - Groups users by their directly assigned roles. A user with multiple roles
   *       appears in each role's group.
   *   <li>{@code baseRole} - Groups users by ancestor roles in the inheritance chain. For each
   *       user, all ancestor roles of the user's direct roles are collected, and the user appears
   *       in each ancestor role's group. If a direct role has no ancestors (it is a root-level
   *       role), the user appears under that direct role's group.
   *   <li>{@code null} or blank - No grouping; all matching users are listed in a single table.
   * </ul>
   *
   * @param request report query filters and grouping option
   * @param locale the locale for i18n label resolution
   * @return PDF document as a byte array
   */
  public byte[] generateUserListReport(UserListReportRequest request, Locale locale) {
    log.debug("Generating user list report with filters: {}", request);

    List<MstUser> users =
        userService.getUserList(request.getUsername(), null).stream()
            .limit(appProperties.getReport().getMaxExportRecords())
            .toList();

    List<UserReportItem> userItems = buildUserReportItems(users);
    Map<String, Object> variables =
        buildUserListReportVariables(userItems, request.getGroupBy(), locale);

    return pdfGeneratorService.generatePdf("reports/user-list-report", variables, locale);
  }

  private List<UserReportItem> buildUserReportItems(List<MstUser> users) {
    List<UserReportItem> items = new ArrayList<>();
    for (MstUser user : users) {
      List<MstRole> roles = roleService.getRoleListByUserId(user.getId());
      items.add(UserReportItem.from(user, roles));
    }
    return items;
  }

  private Map<String, Object> buildUserListReportVariables(
      List<UserReportItem> userItems, String groupBy, Locale locale) {
    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("generatedAt", java.time.LocalDateTime.now().format(DATE_FORMATTER));
    variables.put("totalRecords", userItems.size());
    variables.put("groupBy", groupBy);

    if ("role".equals(groupBy)) {
      variables.put("groups", groupByRole(userItems));
    } else if ("baseRole".equals(groupBy)) {
      variables.put("groups", groupByBaseRole(userItems));
    } else {
      variables.put("users", userItems);
    }

    return variables;
  }

  private List<UserListGroupDto> groupByRole(List<UserReportItem> userItems) {
    LinkedHashMap<Long, MstRole> roleLookup = new LinkedHashMap<>();
    LinkedHashMap<Long, List<UserReportItem>> grouped = new LinkedHashMap<>();

    for (UserReportItem item : userItems) {
      for (MstRole role : item.getRoles()) {
        roleLookup.putIfAbsent(role.getId(), role);
        grouped.computeIfAbsent(role.getId(), k -> new ArrayList<>()).add(item);
      }
    }

    List<UserListGroupDto> groups = new ArrayList<>();
    for (Map.Entry<Long, List<UserReportItem>> entry : grouped.entrySet()) {
      MstRole role = roleLookup.get(entry.getKey());
      groups.add(new UserListGroupDto(role, entry.getValue()));
    }
    return groups;
  }

  private List<UserListGroupDto> groupByBaseRole(List<UserReportItem> userItems) {
    LinkedHashMap<Long, MstRole> roleLookup = new LinkedHashMap<>();
    LinkedHashMap<Long, List<UserReportItem>> grouped = new LinkedHashMap<>();

    for (UserReportItem item : userItems) {
      List<Long> directRoleIds = item.getRoles().stream().map(MstRole::getId).toList();
      Set<Long> baseRoleIds = roleService.resolveAncestorRoleIds(directRoleIds);

      if (baseRoleIds.isEmpty()) {
        baseRoleIds = new LinkedHashSet<>(directRoleIds);
      }

      Map<Long, MstRole> baseRoles =
          roleService.getRolesByIds(baseRoleIds.stream().map(Long::intValue).toList()).stream()
              .collect(Collectors.toMap(MstRole::getId, r -> r));

      for (Long baseRoleId : baseRoleIds) {
        MstRole baseRole = baseRoles.get(baseRoleId);
        if (baseRole != null) {
          roleLookup.putIfAbsent(baseRoleId, baseRole);
          grouped.computeIfAbsent(baseRoleId, k -> new ArrayList<>()).add(item);
        }
      }
    }

    List<UserListGroupDto> groups = new ArrayList<>();
    for (Map.Entry<Long, List<UserReportItem>> entry : grouped.entrySet()) {
      MstRole role = roleLookup.get(entry.getKey());
      groups.add(new UserListGroupDto(role, entry.getValue()));
    }
    return groups;
  }
}
