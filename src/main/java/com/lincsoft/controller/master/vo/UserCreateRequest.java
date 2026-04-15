package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User create request VO.
 *
 * @param username Username
 * @param password Password
 * @param status User status
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserCreateRequest(
    @NotBlank(message = "Username is required")
        @Size(max = 64, message = "Username must be at most 64 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username must contain only letters, digits, and underscores")
        String username,
    @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,
    @Pattern(regexp = "^[01]$", message = "Status must be 0 (inactive) or 1 (active)")
        String status) {}
