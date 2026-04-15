package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * User update request VO.
 *
 * @param id User ID
 * @param username Username
 * @param password Password (optional, only update if provided)
 * @param status User status
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-04-15
 */
public record UserUpdateRequest(
    @NotNull(message = "User ID is required") Long id,
    @Size(max = 64, message = "Username must be at most 64 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]*$",
            message = "Username must contain only letters, digits, and underscores")
        String username,
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,
    @Pattern(regexp = "^[01]$", message = "Status must be 0 (inactive) or 1 (active)")
        String status,
    @NotNull(message = "Version is required") Integer version) {}
