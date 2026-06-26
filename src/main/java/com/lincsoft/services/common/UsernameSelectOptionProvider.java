package com.lincsoft.services.common;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.entity.master.MstPosition;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.mapper.master.MstUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for usernames.
 *
 * <p>Provides user select options with {@code username} as value and {@code realName} as label. All
 * non-deleted users are included, regardless of status, so log filters can match historical
 * records.
 *
 * @author 林创科技
 * @since 2026-06-16
 */
@Component
@RequiredArgsConstructor
public class UsernameSelectOptionProvider implements SelectOptionProvider {

  private final MstUserMapper userMapper;

  @Override
  public String getType() {
    return "username";
  }

  @Override
  public List<SelectOption> getOptions() {
    List<MstUser> users =
        userMapper.selectJoinList(
            MstUser.class,
            new MPJLambdaWrapper<MstUser>()
                .select(MstUser::getUsername)
                .select(MstUser::getRealName)
                .selectAs(MstPosition::getPositionName, MstUser::getPositionName)
                .leftJoin(MstPosition.class, MstPosition::getId, MstUser::getPositionId));

    return users.stream()
        .map(
            user -> SelectOption.of(user.getUsername(), user.getRealName(), user.getPositionName()))
        .toList();
  }
}
