package com.lincsoft.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Forgot password request VO.
 *
 * @param usernameOrEmail Username or email address
 * @author 林创科技
 * @since 2026-05-29
 */
public record ForgotPasswordRequest(
    @NotBlank(message = "Username or email is required")
        @Size(max = 128, message = "Input must be at most 128 characters")
        String usernameOrEmail) {}
