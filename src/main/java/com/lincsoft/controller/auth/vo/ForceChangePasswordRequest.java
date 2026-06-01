package com.lincsoft.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Force change password request VO.
 *
 * <p>Used by INACTIVE users who must change their password on first login.
 *
 * @param newPassword The new password to set.
 * @author 林创科技
 * @since 2026-06-01
 */
public record ForceChangePasswordRequest(
    @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword) {}
