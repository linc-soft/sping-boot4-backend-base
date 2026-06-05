package com.lincsoft.controller.master.vo;

import com.lincsoft.constant.SexType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Employee create request VO.
 *
 * @param username Username (required, same validation as UserCreateRequest)
 * @param email Email address (required, same validation as UserCreateRequest)
 * @param nickname Display name (required)
 * @param mobile Mobile phone
 * @param sex Gender
 * @param hiredDate Hire date (yyyy-MM-dd)
 * @param roleIds Role IDs to assign
 * @param remark Remark
 * @author lincsoft
 * @since 2026-06-03
 */
public record EmployeeCreateRequest(
    @NotBlank(message = "Username is required")
        @Size(max = 64, message = "Username must be at most 64 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username must contain only letters, digits, and underscores")
        String username,
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 128, message = "Email must be at most 128 characters")
        String email,
    @NotBlank(message = "Nickname is required") String nickname,
    String mobile,
    SexType sex,
    String hiredDate,
    List<Integer> roleIds,
    String remark) {}
