package com.lincsoft.dto.master;

import com.lincsoft.entity.master.Employee;
import com.lincsoft.entity.master.MstUser;
import java.util.List;

/**
 * DTO combining MstUser and Employee data.
 *
 * @param user The user entity
 * @param employee The employee entity
 * @param roleIds Directly assigned role IDs
 * @author lincsoft
 * @since 2026-06-03
 */
public record EmployeeWithProfile(MstUser user, Employee employee, List<Long> roleIds) {}
