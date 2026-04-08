package com.lincsoft.services.master;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.mapper.master.MstRoleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Role service
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class RoleService {
  private final MstRoleMapper roleMapper;

  /**
   * Get role list by user ID
   *
   * @param userId User ID
   * @return List of roles
   */
  public List<MstRole> getRoleListByUserId(Long userId) {
    return roleMapper.selectJoinList(
        MstRole.class,
        new MPJLambdaWrapper<MstRole>()
            .selectAll(MstRole.class)
            .innerJoin(MstUserRole.class, MstUserRole::getRoleId, MstRole::getId)
            .eq(MstUserRole::getUserId, userId));
  }
}
