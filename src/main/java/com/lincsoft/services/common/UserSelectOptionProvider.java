package com.lincsoft.services.common;

import com.lincsoft.annotation.SelectOptionPermission;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.mapper.master.MstUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for users.
 *
 * <p>Provides user select options with {@code id} as value and {@code username} as label.
 *
 * <p>Requires {@code USER_VIEW} permission since user options are typically needed when managing
 * roles (e.g., assigning users to roles).
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@Component
@SelectOptionPermission("USER_VIEW")
@RequiredArgsConstructor
public class UserSelectOptionProvider implements SelectOptionProvider {

  private final MstUserMapper userMapper;

  @Override
  public String getType() {
    return "user";
  }

  @Override
  public List<SelectOption> getOptions() {
    return userMapper.selectList(null).stream()
        .map(user -> new SelectOption(user.getId(), user.getUsername(), null))
        .toList();
  }
}
