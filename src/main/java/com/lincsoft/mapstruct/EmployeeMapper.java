package com.lincsoft.mapstruct;

import com.lincsoft.controller.master.vo.*;
import com.lincsoft.dto.master.EmployeeWithProfile;
import com.lincsoft.entity.master.Employee;
import com.lincsoft.entity.master.MstUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Employee mapper for converting between VO and entity.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Mapper(componentModel = "spring")
public interface EmployeeMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  @Mapping(target = "version", ignore = true)
  MstUser toUserEntity(SaveEmployeeRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createBy", ignore = true)
  @Mapping(target = "createAt", ignore = true)
  @Mapping(target = "updateBy", ignore = true)
  @Mapping(target = "updateAt", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  Employee toEmployeeEntity(SaveEmployeeRequest request);

  @Mapping(target = "id", source = "user.id")
  @Mapping(target = "username", source = "user.username")
  @Mapping(target = "email", source = "user.email")
  @Mapping(target = "status", source = "user.status")
  @Mapping(target = "roleIds", source = "roleIds")
  @Mapping(target = "version", source = "user.version")
  @Mapping(target = "nickname", source = "employee.nickname")
  @Mapping(target = "mobile", source = "employee.mobile")
  @Mapping(target = "sex", source = "employee.sex")
  @Mapping(target = "hiredDate", source = "employee.hiredDate")
  @Mapping(target = "remark", source = "employee.remark")
  @Mapping(target = "totalAnnualDays", ignore = true)
  @Mapping(target = "usedAnnualDays", ignore = true)
  @Mapping(target = "remainAnnualDays", ignore = true)
  @Mapping(target = "otherLeaveDays", ignore = true)
  EmployeeResponse toResponse(EmployeeWithProfile dto);

  EmployeeListResponseItem toListResponseItem(MstUser user);
}
