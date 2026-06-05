package com.lincsoft.controller.master.vo;

import com.lincsoft.annotation.ValidEnum;
import com.lincsoft.constant.SexType;
import com.lincsoft.constant.UserStatusEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Employee update request VO.
 *
 * @param id User ID (required)
 * @param version Version for optimistic locking (required)
 * @param email Email address (optional, null means no change)
 * @param password Password (optional, only update if provided)
 * @param status User status (optional, validated against UserStatusEnum)
 * @param nickname Display name (optional)
 * @param mobile Mobile phone (optional)
 * @param sex Gender (optional)
 * @param hiredDate Hire date (yyyy-MM-dd, optional)
 * @param roleIds Role IDs (optional, if provided will replace existing role assignments; null means
 *     keep current roles unchanged)
 * @param remark Remark (optional)
 * @author lincsoft
 * @since 2026-06-03
 */
public record EmployeeUpdateRequest(
    @NotNull(message = "User ID is required") Long id,
    @NotNull(message = "Version is required") Integer version,
    @Email @Size(max = 128, message = "Email must be at most 128 characters") String email,
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,
    @ValidEnum(UserStatusEnum.class) String status,
    String nickname,
    String mobile,
    SexType sex,
    String hiredDate,
    List<Integer> roleIds,
    String remark) {}
