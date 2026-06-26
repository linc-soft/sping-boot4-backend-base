package com.lincsoft.controller.system.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource update request VO.
 *
 * @param id Resource ID
 * @param resourceCode Business code (unique)
 * @param resourceName i18n key for display name
 * @param type 0=directory, 1=page, 2=button
 * @param parentId Parent resource ID (0 = top level)
 * @param routePath Route path (type=1 only)
 * @param icon Menu icon (mdi-*)
 * @param sortOrder Sort order among siblings
 * @param roleCode Role code for visibility (NULL for directories)
 * @param status Status (0 disabled / 1 enabled)
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-17
 */
public record ResourceUpdateRequest(
    @NotNull(message = "Resource ID is required") Long id,
    @NotBlank(message = "Resource code is required")
        @Size(max = 128, message = "Resource code must be at most 128 characters")
        String resourceCode,
    @NotBlank(message = "Resource name is required")
        @Size(max = 128, message = "Resource name must be at most 128 characters")
        String resourceName,
    @NotNull(message = "Type is required") Integer type,
    @NotNull(message = "Parent ID is required") Long parentId,
    @Size(max = 255, message = "Route path must be at most 255 characters") String routePath,
    @Size(max = 64, message = "Icon must be at most 64 characters") String icon,
    Integer sortOrder,
    @Size(max = 64, message = "Role code must be at most 64 characters") String roleCode,
    @Size(max = 1, message = "Status must be a single character") String status,
    @NotNull(message = "Version is required") Integer version) {}
