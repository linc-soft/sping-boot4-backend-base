package com.lincsoft.services.common;

import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.mapper.master.MstRoleMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for roles.
 *
 * <p>Provides role select options with {@code id} as value and {@code roleName} as label.
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@Component
@RequiredArgsConstructor
public class RoleSelectOptionProvider implements SelectOptionProvider {

  private final MstRoleMapper roleMapper;

  @Override
  public String getType() {
    return "role";
  }

  @Override
  public List<SelectOption> getOptions() {
    return roleMapper.selectList(null).stream()
        .map(role -> new SelectOption(role.getId(), role.getRoleName()))
        .toList();
  }
}
