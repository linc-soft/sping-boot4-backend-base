package com.lincsoft.dto.master;

import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * User report item DTO for PDF template rendering.
 *
 * <p>Carries user data together with the user's directly assigned roles for use in Thymeleaf PDF
 * templates. Excludes sensitive fields (e.g., password) from the entity.
 *
 * @author 林创科技
 * @since 2026-05-24
 */
@Data
public class UserReportItem {
  private Long id;
  private String username;
  private String status;
  private List<MstRole> roles;
  private LocalDateTime createAt;
  private LocalDateTime updateAt;

  public static UserReportItem from(MstUser user, List<MstRole> roles) {
    UserReportItem item = new UserReportItem();
    item.setId(user.getId());
    item.setUsername(user.getUsername());
    item.setStatus(user.getStatus());
    item.setRoles(roles);
    item.setCreateAt(user.getCreateAt());
    item.setUpdateAt(user.getUpdateAt());
    return item;
  }
}
