package com.lincsoft.controller.master.vo;

import com.lincsoft.constant.SexType;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Employee create/update request VO.
 *
 * @param username Username (required for create, ignored for update)
 * @param password Password (required for create, optional for update)
 * @param email Email address
 * @param status User status
 * @param roleIds Role IDs
 * @param nickname Display name
 * @param mobile Mobile phone
 * @param sex Gender
 * @param hiredDate Hire date (yyyy-MM-dd)
 * @param remark Remark
 * @author lincsoft
 * @since 2026-06-03
 */
public record SaveEmployeeRequest(
    @Size(min = 4, max = 64, message = "Username must be between 4 and 64 characters")
        String username,
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,
    String email,
    String status,
    List<Integer> roleIds,
    @NotBlank(message = "Nickname is required") String nickname,
    String mobile,
    SexType sex,
    String hiredDate,
    String remark) {}
