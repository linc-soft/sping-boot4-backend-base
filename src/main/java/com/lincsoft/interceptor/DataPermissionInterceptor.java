package com.lincsoft.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.lincsoft.annotation.DataPermissionTable;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.PermissionBit;
import com.lincsoft.constant.ResourceType;
import com.lincsoft.services.master.DataPermissionService;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus inner interceptor for automatic data permission SQL injection.
 *
 * <p>Intercepts all SELECT statements and injects data permission WHERE conditions for tables whose
 * entity class is annotated with {@link DataPermissionTable}.
 *
 * <p>Injected condition logic:
 *
 * <ul>
 *   <li>User has ALL-scope: no condition injected (pass-through)
 *   <li>No accessible dept IDs AND no row-level grants: inject {@code AND 1=0} (deny all)
 *   <li>Only dept IDs: inject {@code AND alias.dept_id IN (...)}
 *   <li>Only row-level grants: inject {@code AND alias.id IN (...)}
 *   <li>Both: inject {@code AND (alias.dept_id IN (...) OR alias.id IN (...))}
 * </ul>
 *
 * <p>ID values are always {@code Long} (numeric), so string concatenation is safe from SQL
 * injection here.
 *
 * <p>This interceptor must be registered in {@code MyBatisPlusConfig} before the pagination
 * interceptor.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataPermissionInterceptor implements InnerInterceptor {

  private final DataPermissionService dataPermissionService;

  /**
   * Cache mapping table name → {@link DataPermissionTable} annotation (or null if not annotated).
   * Avoids repeated classpath scanning per query.
   */
  private final Map<String, DataPermissionTable> tableAnnotationCache = new ConcurrentHashMap<>();

  /**
   * Intercept SELECT statements and inject data permission conditions.
   *
   * <p>Skips injection when:
   *
   * <ul>
   *   <li>No user ID in thread-local holder (unauthenticated request)
   *   <li>The target table is not annotated with {@link DataPermissionTable}
   *   <li>The user has ALL-scope access
   * </ul>
   */
  @Override
  @SuppressWarnings("rawtypes")
  public void beforeQuery(
      Executor executor,
      MappedStatement ms,
      Object parameter,
      RowBounds rowBounds,
      ResultHandler resultHandler,
      BoundSql boundSql) {

    // Skip if no user ID in thread-local holder (unauthenticated request)
    Long userId = DataPermissionUserHolder.getCurrentUserId();
    if (userId == null) {
      return;
    }

    String originalSql = boundSql.getSql().trim();
    String enrichedSql = injectPermissionCondition(originalSql, userId, ms.getId());

    if (!enrichedSql.equals(originalSql)) {
      // Reflectively update the SQL in BoundSql
      try {
        java.lang.reflect.Field sqlField = BoundSql.class.getDeclaredField("sql");
        sqlField.setAccessible(true);
        sqlField.set(boundSql, enrichedSql);
      } catch (Exception e) {
        log.warn("Failed to inject data permission SQL condition: {}", e.getMessage());
      }
    }
  }

  /**
   * Inject data permission conditions into the given SQL string.
   *
   * <p>Uses simple string-based table name extraction and condition appending. ID values are always
   * {@code Long} (numeric), so concatenation is safe from SQL injection.
   *
   * @param sql original SQL (trimmed)
   * @param userId current user ID
   * @param mappedStatementId mapper statement ID (used for logging)
   * @return enriched SQL with permission conditions, or original SQL if no injection needed
   */
  private String injectPermissionCondition(String sql, Long userId, String mappedStatementId) {
    try {
      // Extract the main table name from the FROM clause (simple heuristic)
      String tableName = extractTableName(sql);
      if (tableName == null) {
        return sql;
      }

      // Look up the @DataPermissionTable annotation for this table
      DataPermissionTable annotation = resolveAnnotation(tableName);
      if (annotation == null) {
        return sql;
      }

      ResourceType resourceType = annotation.resourceType();
      String deptField = annotation.deptField();

      // Resolve dept IDs for this user
      Set<Long> deptIds = dataPermissionService.resolveAccessibleDeptIds(userId);

      // ALL-scope: skip filtering
      if (deptIds.contains(CommonConstants.DATA_PERM_ALL_SCOPE_SENTINEL)) {
        return sql;
      }

      // Resolve row-level resource IDs
      Set<Long> resourceIds =
          dataPermissionService.resolveGrantedResourceIds(userId, resourceType, PermissionBit.READ);

      // Build the permission condition string
      String permCondition = buildPermissionCondition(tableName, deptField, deptIds, resourceIds);

      // Append condition to the SQL
      return appendCondition(sql, permCondition);

    } catch (Exception e) {
      log.warn(
          "DataPermissionInterceptor: failed to inject condition for [{}]: {}",
          mappedStatementId,
          e.getMessage());
      return sql;
    }
  }

  /**
   * Build the permission condition SQL fragment.
   *
   * <p>All ID values are {@code Long} (numeric), so string concatenation is safe.
   *
   * @param tableAlias table alias or name
   * @param deptField column name for dept filtering
   * @param deptIds accessible dept IDs
   * @param resourceIds accessible resource IDs
   * @return SQL condition fragment (without leading AND)
   */
  private String buildPermissionCondition(
      String tableAlias, String deptField, Set<Long> deptIds, Set<Long> resourceIds) {

    if (deptIds.isEmpty() && resourceIds.isEmpty()) {
      // No access at all: deny everything
      return "1=0";
    }

    String deptCondition = null;
    String rowCondition = null;

    if (!deptIds.isEmpty()) {
      String ids = deptIds.stream().map(String::valueOf).collect(Collectors.joining(","));
      deptCondition = tableAlias + "." + deptField + " IN (" + ids + ")";
    }

    if (!resourceIds.isEmpty()) {
      String ids = resourceIds.stream().map(String::valueOf).collect(Collectors.joining(","));
      rowCondition = tableAlias + ".id IN (" + ids + ")";
    }

    if (deptCondition != null && rowCondition != null) {
      return "(" + deptCondition + " OR " + rowCondition + ")";
    }
    return deptCondition != null ? deptCondition : rowCondition;
  }

  /**
   * Append a WHERE condition to the SQL string.
   *
   * <p>Handles both cases: SQL already has a WHERE clause (appends with AND) and SQL without a
   * WHERE clause (appends WHERE). Also handles ORDER BY, GROUP BY, LIMIT clauses.
   *
   * @param sql original SQL
   * @param condition condition to append
   * @return SQL with the condition appended
   */
  private String appendCondition(String sql, String condition) {
    String upperSql = sql.toUpperCase();

    // Find the position to insert the condition (before ORDER BY, GROUP BY, LIMIT, HAVING)
    int insertPos = sql.length();
    for (String keyword : new String[] {"ORDER BY", "GROUP BY", "LIMIT", "HAVING"}) {
      int pos = upperSql.lastIndexOf(keyword);
      if (pos > 0 && pos < insertPos) {
        insertPos = pos;
      }
    }

    String prefix = sql.substring(0, insertPos).trim();
    String suffix = sql.substring(insertPos);

    if (upperSql.contains(" WHERE ")) {
      return prefix + " AND " + condition + (suffix.isEmpty() ? "" : " " + suffix.trim());
    } else {
      return prefix + " WHERE " + condition + (suffix.isEmpty() ? "" : " " + suffix.trim());
    }
  }

  /**
   * Extract the main table name from a SELECT SQL string.
   *
   * <p>Uses a simple regex to find the table name after the FROM keyword.
   *
   * @param sql SQL string
   * @return table name, or null if not found
   */
  private String extractTableName(String sql) {
    // Match: FROM table_name [alias] [WHERE|JOIN|ORDER|GROUP|LIMIT|;]
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
                "(?i)\\bFROM\\s+`?(\\w+)`?(?:\\s+(?:AS\\s+)?`?(\\w+)`?)?",
                java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(sql);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  /**
   * Resolve the {@link DataPermissionTable} annotation for the given table name.
   *
   * <p>Scans all entity classes in the {@code com.lincsoft.entity} package to find the one mapped
   * to the given table name. Results are cached to avoid repeated scanning.
   *
   * @param tableName database table name
   * @return the annotation, or null if the table is not annotated
   */
  private DataPermissionTable resolveAnnotation(String tableName) {
    return tableAnnotationCache.computeIfAbsent(
        tableName,
        name -> {
          try {
            org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
                scanner =
                    new org.springframework.context.annotation
                        .ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(
                new org.springframework.core.type.filter.AnnotationTypeFilter(
                    DataPermissionTable.class));
            for (org.springframework.beans.factory.config.BeanDefinition bd :
                scanner.findCandidateComponents("com.lincsoft.entity")) {
              Class<?> clazz = Class.forName(bd.getBeanClassName());
              com.baomidou.mybatisplus.annotation.TableName tableNameAnnotation =
                  clazz.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
              if (tableNameAnnotation != null && tableNameAnnotation.value().equals(name)) {
                return clazz.getAnnotation(DataPermissionTable.class);
              }
            }
          } catch (Exception e) {
            log.warn(
                "Failed to resolve @DataPermissionTable for table [{}]: {}", name, e.getMessage());
          }
          // Cache null to avoid repeated scanning for non-annotated tables
          return null;
        });
  }

  /**
   * Resolve the {@link DataPermissionTable} annotation for the given table name.
   *
   * <p>(duplicate comment removed - see resolveAnnotation above)
   */
}
