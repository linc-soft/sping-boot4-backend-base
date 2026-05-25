package com.lincsoft.dto.master;

import com.lincsoft.entity.master.MstUser;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * User report item DTO for PDF template rendering.
 *
 * <p>Carries a subset of user fields for use in Thymeleaf PDF templates. Role data is no longer
 * included here; role grouping is handled separately via {@link
 * com.lincsoft.dto.report.UserReportRow} and the grouping logic in report data fetchers.
 *
 * <p>Excludes sensitive fields (e.g., password) from the entity.
 *
 * @author 林创科技
 * @since 2026-05-24
 */
@Data
public class UserReportItem {
  private String username;
  private String status;
  private LocalDateTime createAt;
  private LocalDateTime updateAt;
  private String createBy;
  private String updateBy;

  /**
   * Creates a report item from a user entity, copying only the display-safe fields.
   *
   * @param user the user entity to copy from
   * @return a new UserReportItem with fields populated from the entity
   */
  public static UserReportItem from(MstUser user) {
    UserReportItem item = new UserReportItem();
    item.setUsername(user.getUsername());
    item.setStatus(user.getStatus());
    item.setCreateAt(user.getCreateAt());
    item.setUpdateAt(user.getUpdateAt());
    item.setCreateBy(user.getCreateBy());
    item.setUpdateBy(user.getUpdateBy());
    return item;
  }
}
