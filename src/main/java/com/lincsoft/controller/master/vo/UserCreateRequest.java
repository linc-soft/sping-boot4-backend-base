package com.lincsoft.controller.master.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * User create request VO.
 *
 * <p>The {@code status} is not accepted from the client; new users are always created as {@code
 * INACTIVE} and must change their password on first login.
 *
 * @param username Username
 * @param email Email address (required)
 * @param roleIds Role IDs
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
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 128, message = "Email must be at most 128 characters")
        String email,
    List<Integer> roleIds) {}
