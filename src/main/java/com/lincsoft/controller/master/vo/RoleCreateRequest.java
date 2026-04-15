package com.lincsoft.controller.master.vo;

import com.lincsoft.annotation.ValidRoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Role create request VO.
 *
 * @param roleName Role name
 * @param roleCode Role code
 * @param description Role description
 * @author 林创科技
 * @since 2026-04-14
 */
public record RoleCreateRequest(
    @NotBlank(message = "Role name is required")
        @Size(max = 64, message = "Role name must be at most 64 characters")
        String roleName,
    @NotBlank(message = "Role code is required") @ValidRoleCode String roleCode,
    @Size(max = 255, message = "Description must be at most 255 characters") String description) {}
