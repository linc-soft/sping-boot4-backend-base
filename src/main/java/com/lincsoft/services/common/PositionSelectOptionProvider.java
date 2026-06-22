package com.lincsoft.services.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.entity.master.MstPosition;
import com.lincsoft.mapper.master.MstPositionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for positions.
 *
 * <p>Provides position select options with {@code id} as value and {@code positionName} as label.
 * Unlike {@link RoleSelectOptionProvider}, this filters by {@code status='1'} and sorts by {@code
 * sort_order} since positions have explicit status and ordering fields.
 *
 * @author 林创科技
 * @since 2026-06-10
 */
@Component
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
    queryWrapper.eq("status", "1");
    queryWrapper.orderByAsc("sort_order").orderByAsc("id");
    return positionMapper.selectList(queryWrapper).stream()
        .map(pos -> SelectOption.of(pos.getId(), pos.getPositionName(), pos.getPositionCode()))
        .toList();
  }
}
