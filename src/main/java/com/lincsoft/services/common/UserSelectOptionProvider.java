package com.lincsoft.services.common;

import com.github.yulichang.wrapper.MPJLambdaWrapper;
import com.lincsoft.annotation.SelectOptionPermission;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.entity.master.MstPosition;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.mapper.master.MstUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@SelectOptionPermission("USER_READ")
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
