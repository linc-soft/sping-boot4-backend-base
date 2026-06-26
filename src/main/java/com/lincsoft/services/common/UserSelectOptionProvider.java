package com.lincsoft.services.common;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.entity.master.MstPosition;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.mapper.master.MstUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for active users.
 *
 * <p>Provides user select options with {@code id} as value and {@code realName} as label. Only
 * active users ({@code status='1'}) are included, so disabled/inactive users cannot be selected as
 * department leaders.
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
    List<MstUser> users =
        userMapper.selectJoinList(
            MstUser.class,
            new MPJLambdaWrapper<MstUser>()
                .select(MstUser::getId)
                .select(MstUser::getRealName)
                .selectAs(MstPosition::getPositionName, MstUser::getPositionName)
                .eq(MstUser::getStatus, CommonConstants.USER_STATUS_ACTIVE)
                .leftJoin(MstPosition.class, MstPosition::getId, MstUser::getPositionId));

    return users.stream()
        .map(user -> SelectOption.of(user.getId(), user.getRealName(), user.getPositionName()))
        .toList();
  }
}
