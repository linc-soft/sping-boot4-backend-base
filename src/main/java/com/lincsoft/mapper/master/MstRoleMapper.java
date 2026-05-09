package com.lincsoft.mapper.master;

import com.github.yulichang.base.MPJBaseMapper;
import com.lincsoft.entity.master.MstRole;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Role mapping
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Mapper
public interface MstRoleMapper extends MPJBaseMapper<MstRole> {

  /**
   * Recursively find all role IDs that match the given roleCode or inherit from matching roles.
   *
   * <p>Uses MySQL recursive CTE to traverse the inheritance graph downward in a single query.
   *
   * @param roleCode Role code prefix to match
   * @return Set of role IDs (direct matches + all descendants)
   */
  @Select(
      """
      WITH RECURSIVE role_tree AS (
        SELECT r.id FROM mst_role r
        WHERE r.role_code = #{roleCode} AND r.deleted = 0
        UNION ALL
        SELECT ri.child_role_id FROM mst_role_inheritance ri
        INNER JOIN role_tree rt ON ri.parent_role_id = rt.id
        WHERE ri.deleted = 0
      )
      SELECT DISTINCT id FROM role_tree
      """)
  Set<Long> selectRoleIdsRecursiveByRoleCode(@Param("roleCode") String roleCode);
}
