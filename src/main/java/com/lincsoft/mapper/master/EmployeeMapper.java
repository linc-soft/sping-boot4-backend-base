package com.lincsoft.mapper.master;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lincsoft.entity.master.Employee;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for Employee entity.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {}
