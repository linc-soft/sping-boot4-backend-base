package com.lincsoft.controller.master.vo;

/**
 * Role info response VO.
 *
 * @param id Role ID
 * @param roleName Role name
 * @param roleCode Role code
 * @param description Role description
 * @param version Version
 * @author 林创科技
 * @since 2026-04-15
 */
public record RoleInfoResponse(
    Long id, String roleName, String roleCode, String description, Integer version) {}
