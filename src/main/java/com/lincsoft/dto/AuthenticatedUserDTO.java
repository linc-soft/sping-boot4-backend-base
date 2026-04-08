package com.lincsoft.dto;

/**
 * Authenticated user data transfer object.
 *
 * @param username The username of the authenticated user.
 * @param status The status of the authenticated user.
 */
public record AuthenticatedUserDTO(String username, String status) {}
