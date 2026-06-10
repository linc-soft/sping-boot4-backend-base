package com.lincsoft.services.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.SelectOptionPermission;
import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import com.lincsoft.entity.oa.MstDepartment;
import com.lincsoft.mapper.oa.MstDepartmentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Select option provider for departments.
 *
 * <p>Provides department select options with {@code id} as value and {@code deptName} as label.
 * Unlike {@link RoleSelectOptionProvider}, this filters by {@code status='1'} and sorts by {@code
 * sort_order} since departments have explicit status and ordering fields.
 *
 * <p>Requires {@code DEPT_READ} permission.
 *
 * @author 林创科技
 * @since 2026-06-10
 */
@Component
@SelectOptionPermission("DEPT_READ")
@RequiredArgsConstructor
public class DepartmentSelectOptionProvider implements SelectOptionProvider {

  private final MstDepartmentMapper departmentMapper;

  @Override
  public String getType() {
    return "department";
  }

  @Override
  public List<SelectOption> getOptions() {
    QueryWrapper<MstDepartment> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("status", "1");
    queryWrapper.orderByAsc("sort_order").orderByAsc("id");
    return departmentMapper.selectList(queryWrapper).stream()
        .map(dept -> new SelectOption(dept.getId(), dept.getDeptName(), dept.getDeptCode()))
        .toList();
  }
}
