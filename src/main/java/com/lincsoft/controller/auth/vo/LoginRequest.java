package com.lincsoft.controller.auth.vo;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request VO.
 *
 * @param username The username for authentication.
 * @param password The password for authentication.
 * @author 林创科技
 * @since 2026-04-10
 */
public record LoginRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password) {}
