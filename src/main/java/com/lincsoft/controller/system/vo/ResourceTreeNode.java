package com.lincsoft.controller.system.vo;

import java.util.List;

/**
 * Resource tree node response VO.
 *
 * <p>Each node carries its direct children, forming a nested tree. Top-level nodes have {@code
 * parentId} equal to 0.
 *
 * @param id Resource ID
 * @param resourceCode Business code
 * @param resourceName i18n key for display name
 * @param type 0=directory, 1=page, 2=button
 * @param parentId Parent resource ID (0 = top level)
 * @param routePath Route path (type=1 only)
 * @param icon Menu icon (mdi-*)
 * @param sortOrder Sort order among siblings
 * @param roleCode Role code for visibility
 * @param status Status (0 disabled / 1 enabled)
 * @param children Direct child resources
 * @author 林创科技
 * @since 2026-06-17
 */
public record ResourceTreeNode(
    Long id,
    String resourceCode,
    String resourceName,
    Integer type,
    Long parentId,
    String routePath,
    String icon,
    Integer sortOrder,
    String roleCode,
    String status,
    List<ResourceTreeNode> children) {}
