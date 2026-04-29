package com.lincsoft.services.master;

import com.lincsoft.entity.master.MstRole;
import java.util.List;

/**
 * Role with its direct parent role IDs.
 *
 * <p>Internal service-layer DTO that carries a role entity together with its directly inherited
 * parent role IDs, so downstream mappers can populate the {@code parentRoleIds} field of list
 * response VOs without re-querying the inheritance table per role.
 *
 * @param role Role entity
 * @param parentRoleIds Direct parent role IDs (never {@code null}; empty when none)
 * @author 林创科技
 * @since 2026-04-29
 */
public record RoleWithParents(MstRole role, List<Long> parentRoleIds) {}
