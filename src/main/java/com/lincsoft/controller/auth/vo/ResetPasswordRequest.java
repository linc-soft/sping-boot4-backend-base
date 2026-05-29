package com.lincsoft.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Reset password request VO.
 *
 * @param token Reset token from email link
 * @param newPassword New password
 * @author 林创科技
 * @since 2026-05-29
 */
public record ResetPasswordRequest(
    @NotBlank(message = "Token is required") String token,
    @NotBlank(message = "New password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String newPassword) {}
