package com.lincsoft.controller.master.vo;

/**
 * Role list query request VO.
 *
 * @param roleName Role name (partial match)
 * @param roleCode Role code (prefix match)
 * @param description Description (partial match)
 * @param aggregatedOnly Only include aggregated roles (roles with null role_code)
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleListRequest(
    String roleName, String roleCode, String description, Boolean aggregatedOnly) {}
