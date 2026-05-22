package com.lincsoft.controller.pdf.vo;

import jakarta.validation.constraints.Size;

/**
 * Role list PDF generation request VO.
 *
 * @param roleName Role name (1-255 characters, partial match)
 * @param roleCode Role CODE (1-50 characters, exact match)
 * @author 林创科技
 * @since 2026-05-22
 */
public record RoleListPdfRequest(
    @Size(max = 255, message = "角色名称不能超过 255 字符") String roleName,
    @Size(max = 50, message = "角色 CODE 不能超过 50 字符") String roleCode) {}
