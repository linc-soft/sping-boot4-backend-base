package com.lincsoft.services.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.SelectOptionPermission;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.entity.oa.MstPosition;
import com.lincsoft.mapper.oa.MstPositionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for positions, mirroring {@code RoleSelectOptionProvider} with status
 * filter and sort order.
 *
 * @author 林创科技
 * @since 2026-06-10
 */
@Component
@SelectOptionPermission("POSITION_READ")
@RequiredArgsConstructor
public class PositionSelectOptionProvider implements SelectOptionProvider {

  private final MstPositionMapper positionMapper;

  @Override
  public String getType() {
    return "position";
  }

  @Override
  public List<SelectOption> getOptions() {
    QueryWrapper<MstPosition> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("status", "1").orderByAsc("sort_order").orderByAsc("id");
    return positionMapper.selectList(queryWrapper).stream()
        .map(pos -> new SelectOption(pos.getId(), pos.getPositionName(), pos.getPositionCode()))
        .toList();
  }
}
