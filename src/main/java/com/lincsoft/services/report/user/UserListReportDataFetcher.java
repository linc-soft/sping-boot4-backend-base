package com.lincsoft.services.report.user;

import com.lincsoft.annotation.OperationLog;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.Module;
import com.lincsoft.constant.OperationType;
import com.lincsoft.constant.SubModule;
import com.lincsoft.controller.master.vo.UserListReportRequest;
import com.lincsoft.dto.master.UserListGroupDto;
import com.lincsoft.dto.master.UserReportItem;
import com.lincsoft.dto.report.UserReportRow;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.mapper.report.ReportMapper;
import com.lincsoft.services.report.ReportDataFetcher;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fetches and assembles data for the user list PDF report.
 *
 * <p>Uses optimized single-SQL queries to avoid N+1 problems:
 *
 * <ul>
 *   <li><b>ungrouped / group-by-role</b>: LEFT JOIN mst_user → mst_user_role → mst_role (1 query)
 *   <li><b>group-by-baseRole</b>: recursive CTE + JOIN to resolve role ancestors (1 query)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-05-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserListReportDataFetcher implements ReportDataFetcher<UserListReportRequest> {

  private final ReportMapper reportMapper;
  private final AppProperties appProperties;

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String TEMPLATE_NAME = "reports/user-list-report";

  @Override
  public String getTemplateName() {
    return TEMPLATE_NAME;
  }

  /**
   * Fetches and assembles user list report data based on the grouping strategy.
   *
   * <ul>
   *   <li><b>baseRole</b> - queries via recursive CTE and groups by ancestor role
   *   <li><b>role</b> - queries direct roles and groups by them
   *   <li><b>ungrouped</b> (null/blank) - returns a flat user list without grouping
   * </ul>
   *
   * @param request filter and grouping options
   * @param locale locale for report localization
   * @return template variables map with generated timestamp, group strategy, total records, and
   *     grouped user data
   */
  @Override
  @OperationLog(
      module = Module.MASTER,
      subModule = SubModule.USER,
      type = OperationType.EXPORT,
      description = "User list report exported: #{#request}")
  public Map<String, Object> fetchData(UserListReportRequest request, Locale locale) {
    log.debug("Fetching user list report data, filters: {}", request);

    String username =
        (request.getUsername() != null && !request.getUsername().isBlank())
            ? request.getUsername()
            : null;
    int maxRecords = appProperties.getReport().getMaxExportRecords();
    String groupBy = request.getGroupBy();

    Map<String, Object> variables = new LinkedHashMap<>();
    variables.put("generatedAt", LocalDateTime.now().format(DATE_FORMATTER));
    variables.put("groupBy", groupBy);

    if ("baseRole".equals(groupBy)) {
      List<UserReportRow> rows = reportMapper.selectUserReportRowsByBaseRole(username, maxRecords);
      List<UserListGroupDto> groups = groupByBaseRole(rows);
      variables.put("totalRecords", countDistinctUsers(rows));
      variables.put("groups", groups);
    } else {
      List<UserReportRow> rows = reportMapper.selectUserReportRows(username, maxRecords);
      List<UserReportItem> items = toUngroupedItems(rows);
      variables.put("totalRecords", items.size());

      if ("role".equals(groupBy)) {
        variables.put("groups", groupByRole(rows));
      } else {
        variables.put("users", items);
      }
    }

    return variables;
  }

  /**
   * Converts joined rows into distinct user items for ungrouped display.
   *
   * <p>Since the SQL JOIN produces one row per user-role pair, a single user may appear in multiple
   * rows. This method deduplicates by user ID, keeping only the first occurrence.
   *
   * @param rows the raw joined query results
   * @return deduplicated list of user report items
   */
  private List<UserReportItem> toUngroupedItems(List<UserReportRow> rows) {
    LinkedHashMap<Long, UserReportItem> userMap = new LinkedHashMap<>();
    for (UserReportRow row : rows) {
      userMap.computeIfAbsent(row.getUserId(), _ -> toNewItem(row));
    }
    return new ArrayList<>(userMap.values());
  }

  /**
   * Groups users by their directly assigned roles.
   *
   * <p>Processes the raw JOIN results to produce role-grouped user collections. A user with
   * multiple roles appears in each role's group. Users without any role are silently excluded from
   * the grouped output (they are only registered in userLookup to avoid duplicates but never added
   * to any group).
   *
   * @param rows the raw joined query results
   * @return list of role-grouped user collections
   */
  private List<UserListGroupDto> groupByRole(List<UserReportRow> rows) {
    LinkedHashMap<Long, MstRole> roleLookup = new LinkedHashMap<>();
    LinkedHashMap<Long, List<UserReportItem>> grouped = new LinkedHashMap<>();
    LinkedHashMap<Long, UserReportItem> userLookup = new LinkedHashMap<>();

    for (UserReportRow row : rows) {
      if (row.getRoleId() == null) {
        userLookup.computeIfAbsent(row.getUserId(), _ -> toNewItem(row));
        continue;
      }
      roleLookup.putIfAbsent(row.getRoleId(), toRole(row));
      userLookup.computeIfAbsent(row.getUserId(), _ -> toNewItem(row));
      grouped
          .computeIfAbsent(row.getRoleId(), _ -> new ArrayList<>())
          .add(userLookup.get(row.getUserId()));
    }

    List<UserListGroupDto> groups = new ArrayList<>();
    for (Map.Entry<Long, List<UserReportItem>> entry : grouped.entrySet()) {
      MstRole role = roleLookup.get(entry.getKey());
      groups.add(new UserListGroupDto(role, entry.getValue()));
    }
    return groups;
  }

  /**
   * Groups users by their base (ancestor) roles using recursive CTE results.
   *
   * <p>The SQL query already resolves each direct role to its ancestor via the recursive CTE, so
   * each row already carries the final base role. This method simply groups rows by the resolved
   * base role ID.
   *
   * @param rows the raw joined query results with base roles pre-resolved by the CTE
   * @return list of base-role-grouped user collections
   */
  private List<UserListGroupDto> groupByBaseRole(List<UserReportRow> rows) {
    LinkedHashMap<Long, MstRole> roleLookup = new LinkedHashMap<>();
    LinkedHashMap<Long, List<UserReportItem>> grouped = new LinkedHashMap<>();
    LinkedHashMap<Long, UserReportItem> userLookup = new LinkedHashMap<>();

    for (UserReportRow row : rows) {
      Long baseRoleId = row.getRoleId();
      MstRole baseRole = toBaseRole(row);

      roleLookup.putIfAbsent(baseRoleId, baseRole);

      UserReportItem item = userLookup.computeIfAbsent(row.getUserId(), _ -> toNewItem(row));

      grouped.computeIfAbsent(baseRoleId, _ -> new ArrayList<>()).add(item);
    }

    List<UserListGroupDto> groups = new ArrayList<>();
    for (Map.Entry<Long, List<UserReportItem>> entry : grouped.entrySet()) {
      MstRole role = roleLookup.get(entry.getKey());
      groups.add(new UserListGroupDto(role, entry.getValue()));
    }
    return groups;
  }

  /**
   * Counts distinct users from joined rows.
   *
   * <p>In grouped modes, a single user may produce multiple rows (one per role), so the row count
   * overstates the actual user count. This method deduplicates by user ID.
   *
   * @param rows the raw joined query results
   * @return number of distinct users
   */
  private int countDistinctUsers(List<UserReportRow> rows) {
    Set<Long> userIds = new HashSet<>();
    for (UserReportRow row : rows) {
      userIds.add(row.getUserId());
    }
    return userIds.size();
  }

  /**
   * Extracts a direct role entity from a joined row.
   *
   * <p>Used in {@link #groupByRole} to build the role lookup map from the direct role columns in
   * the row. Although identical in implementation to {@link #toBaseRole}, this method is kept
   * separate for semantic clarity: it extracts the <b>direct</b> role, not the ancestor role.
   *
   * @param row the joined query result row
   * @return a MstRole populated from the row's role columns
   */
  private static MstRole toRole(UserReportRow row) {
    MstRole role = new MstRole();
    role.setId(row.getRoleId());
    role.setRoleName(row.getRoleName());
    role.setRoleCode(row.getRoleCode());
    return role;
  }

  /**
   * Extracts a base (ancestor) role entity from a joined row.
   *
   * <p>Used in {@link #groupByBaseRole} to build the role lookup map. In the base-role query, the
   * role columns are already resolved to the ancestor role via the recursive CTE. Although
   * identical in implementation to {@link #toRole}, this method is kept separate for semantic
   * clarity: it extracts the <b>base</b> (ancestor) role, not the direct role.
   *
   * @param row the joined query result row
   * @return a MstRole populated from the row's role columns
   */
  private static MstRole toBaseRole(UserReportRow row) {
    MstRole role = new MstRole();
    role.setId(row.getRoleId());
    role.setRoleName(row.getRoleName());
    role.setRoleCode(row.getRoleCode());
    return role;
  }

  /**
   * Converts a joined row into a user report item for template rendering.
   *
   * <p>Extracts only the user-related fields from the row, ignoring role columns. Used in multiple
   * grouping and deduplication methods to ensure consistent item creation.
   *
   * @param row the joined query result row
   * @return a UserReportItem populated from the row's user columns
   */
  private static UserReportItem toNewItem(UserReportRow row) {
    UserReportItem item = new UserReportItem();
    item.setUsername(row.getUsername());
    item.setStatus(row.getStatus());
    item.setCreateBy(row.getCreateBy());
    item.setUpdateBy(row.getUpdateBy());
    item.setCreateAt(row.getCreateAt());
    item.setUpdateAt(row.getUpdateAt());
    return item;
  }
}
