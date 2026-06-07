package com.lincsoft.controller.oa.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Employee update request VO.
 *
 * <p>Updates the employee profile only. The linked login account (username, password, roles) is
 * managed separately via the user management module.
 *
 * @param id Employee ID
 * @param employeeNo Employee number
 * @param realName Real name
 * @param deptId Department ID
 * @param positionId Position ID
 * @param managerId Direct supervisor employee ID
 * @param mobile Mobile phone
 * @param email Email address
 * @param gender Gender (0 unknown / 1 male / 2 female)
 * @param idCardNo ID card number
 * @param birthday Date of birth
 * @param hireDate Hire date
 * @param status Employment status (0 left / 1 active / 2 on leave)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-07
 */
public record EmployeeUpdateRequest(
    @NotNull(message = "Employee ID is required") Long id,
    @Size(max = 32, message = "Employee number must be at most 32 characters") String employeeNo,
    @NotBlank(message = "Real name is required")
        @Size(max = 64, message = "Real name must be at most 64 characters")
        String realName,
    Long deptId,
    Long positionId,
    Long managerId,
    @Size(max = 20, message = "Mobile must be at most 20 characters") String mobile,
    @Email(message = "Email must be a valid email address")
        @Size(max = 128, message = "Email must be at most 128 characters")
        String email,
    @Size(max = 1, message = "Gender must be a single character") String gender,
    @Size(max = 32, message = "ID card number must be at most 32 characters") String idCardNo,
    LocalDate birthday,
    LocalDate hireDate,
    @Size(max = 1, message = "Status must be a single character") String status,
    @NotNull(message = "Version is required") Integer version) {}
