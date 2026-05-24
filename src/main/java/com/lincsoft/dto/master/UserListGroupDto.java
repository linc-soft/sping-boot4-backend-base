package com.lincsoft.dto.master;

import com.lincsoft.entity.master.MstRole;
import java.util.List;

/**
 * User list group DTO for PDF template rendering.
 *
 * <p>Represents a single group in the grouped user list report, containing the group's role (either
 * a directly assigned role or a base/ancestor role) and the list of users belonging to that group.
 *
 * @param role the role that defines this group
 * @param users the list of users in this group
 * @author 林创科技
 * @since 2026-05-24
 */
public record UserListGroupDto(MstRole role, List<UserReportItem> users) {}
