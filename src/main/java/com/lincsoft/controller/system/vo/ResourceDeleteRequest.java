package com.lincsoft.controller.system.vo;

import jakarta.validation.constraints.NotNull;

/**
 * Resource delete request VO.
 *
 * @param id Resource ID
 * @param version Version for optimistic locking
 * @author 林创科技
 * @since 2026-06-17
 */
public record ResourceDeleteRequest(
    @NotNull(message = "Resource ID is required") Long id,
    @NotNull(message = "Version is required") Integer version) {}
