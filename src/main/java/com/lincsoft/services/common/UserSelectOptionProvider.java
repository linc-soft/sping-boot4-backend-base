package com.lincsoft.services.common;

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
 * @author 林创科技
 * @since 2026-04-21
 */
@Component
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
        .map(user -> new SelectOption(user.getId(), user.getUsername()))
        .toList();
  }
}
