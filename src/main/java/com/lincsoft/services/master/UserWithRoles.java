package com.lincsoft.services.master;

import com.lincsoft.entity.master.MstUser;
import java.util.List;

/**
 * User with its directly assigned role IDs.
 *
 * <p>Internal service-layer DTO that carries a user entity together with the IDs of roles directly
 * assigned to the user, so downstream mappers can populate the {@code roleIds} field of response
 * VOs without re-querying the user-role junction table.
 *
 * @param user User entity
 * @param roleIds Directly assigned role IDs (never {@code null}; empty when none)
 * @author 林创科技
 * @since 2026-05-08
 */
public record UserWithRoles(MstUser user, List<Long> roleIds) {}
