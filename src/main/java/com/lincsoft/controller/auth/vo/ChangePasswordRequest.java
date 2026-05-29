package com.lincsoft.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Change password request VO for authenticated users.
 *
 * @param currentPassword Current password
 * @param newPassword New password
 * @author 林创科技
 * @since 2026-05-29
 */
public record ChangePasswordRequest(
    @NotBlank(message = "Current password is required") String currentPassword,
    @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword) {}
