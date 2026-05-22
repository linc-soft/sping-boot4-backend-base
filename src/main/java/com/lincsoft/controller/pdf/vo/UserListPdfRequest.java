package com.lincsoft.controller.pdf.vo;


/**
 * User list PDF generation request VO.
 *
 * @param username Username (partial match, case-insensitive)
 * @param status User status (must be one of: active, inactive, suspended)
 * @author 林创科技
 * @since 2026-05-22
 */
public record UserListPdfRequest(String username, String status) {}
