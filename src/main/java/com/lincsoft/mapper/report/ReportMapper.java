package com.lincsoft.mapper.report;

import com.lincsoft.dto.report.UserReportRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Report mapper for optimized aggregated queries.
 *
 * <p>Provides single-Sql queries for report data fetching, eliminating N+1 query problems.
 *
 * @author 林创科技
 * @since 2026-05-25
 */
@Mapper
public interface ReportMapper {

  /**
   * Fetches user-role joined data for the user list report (ungrouped or group-by-role mode).
   *
   * <p>Single query: LEFT JOIN mst_user → mst_user_role → mst_role, returning one row per user-role
   * pair. Users without roles still appear with null role columns. Results are ordered by user
   * update_at descending, then role id.
   *
   * @param username optional username filter (SQL LIKE pattern, null means no filter)
   * @param maxRecords maximum number of users to include
   * @return list of UserReportRow where each row represents one user-role relationship
   */
  @Select(
      """
      SELECT
        u.id AS user_id,
        u.username,
        u.status,
        u.create_by,
        u.update_by,
        u.create_at,
        u.update_at,
        r.id AS role_id,
        r.role_name,
        r.role_code
      FROM mst_user u
      LEFT JOIN mst_user_role ur ON u.id = ur.user_id AND ur.deleted = 0
      LEFT JOIN mst_role r ON ur.role_id = r.id AND r.deleted = 0
      WHERE u.deleted = 0
        AND (#{username} IS NULL OR u.username LIKE CONCAT('%', #{username}, '%'))
      ORDER BY u.update_at DESC, r.id
      LIMIT #{maxRecords}
      """)
  List<UserReportRow> selectUserReportRows(
      @Param("username") String username, @Param("maxRecords") int maxRecords);

  /**
   * Fetches user-role-baseRole joined data using recursive CTE for group-by-baseRole mode.
   *
   * <p>Uses a recursive CTE to traverse the role inheritance chain upward, resolving each direct
   * role to its ancestor (base) roles. For roles without ancestors, the direct role itself is used
   * as the base role. The result contains one row per (user, base_role) pair.
   *
   * <p>SQL structure:
   *
   * <ol>
   *   <li>{@code role_ancestors} CTE: recursively finds all ancestor role IDs for each direct role
   *   <li>Main query: joins users with their direct roles, left joins to resolve ancestor (base)
   *       roles via the CTE
   * </ol>
   *
   * @param username optional username filter (SQL LIKE pattern, null means no filter)
   * @param maxRecords maximum number of users to include
   * @return list of UserReportRow where each row represents one user-baseRole relationship
   */
  @Select(
      """
      WITH RECURSIVE role_ancestors AS (
        SELECT child_role_id, parent_role_id
        FROM mst_role_inheritance
        WHERE deleted = 0
        UNION ALL
        SELECT ra.child_role_id, ri.parent_role_id
        FROM role_ancestors ra
        INNER JOIN mst_role_inheritance ri ON ra.parent_role_id = ri.child_role_id AND ri.deleted = 0
      )
      SELECT
        u.id AS user_id,
        u.username,
        u.status,
        u.create_by,
        u.update_by,
        u.create_at,
        u.update_at,
        COALESCE(br.id, r.id) AS role_id,
        COALESCE(br.role_name, r.role_name) AS role_name,
        COALESCE(br.role_code, r.role_code) AS role_code
      FROM mst_user u
      INNER JOIN mst_user_role ur ON u.id = ur.user_id AND ur.deleted = 0
      INNER JOIN mst_role r ON ur.role_id = r.id AND r.deleted = 0
      LEFT JOIN role_ancestors ra ON ur.role_id = ra.child_role_id
      LEFT JOIN mst_role br ON ra.parent_role_id = br.id AND br.deleted = 0
      WHERE u.deleted = 0
        AND (#{username} IS NULL OR u.username LIKE CONCAT('%', #{username}, '%'))
      ORDER BY COALESCE(br.id, r.id), u.update_at DESC
      LIMIT #{maxRecords}
      """)
  List<UserReportRow> selectUserReportRowsByBaseRole(
      @Param("username") String username, @Param("maxRecords") int maxRecords);
}
