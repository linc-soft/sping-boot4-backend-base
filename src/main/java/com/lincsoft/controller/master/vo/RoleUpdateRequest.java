package com.lincsoft.controller.master.vo;

import com.lincsoft.annotation.ValidRoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Role update request VO.
 *
 * @param id Role ID
 * @param roleName Role name
 * @param roleCode Role code
 * @param description Role description
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleUpdateRequest(
    @NotNull(message = "Role ID is required") Long id,
    @NotBlank(message = "Role name is required")
        @Size(max = 64, message = "Role name must be at most 64 characters")
        String roleName,
    @NotBlank(message = "Role code is required") @ValidRoleCode String roleCode,
    @Size(max = 255, message = "Description must be at most 255 characters") String description,
    @NotNull(message = "Version is required") Integer version) {}
