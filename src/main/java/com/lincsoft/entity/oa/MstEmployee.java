package com.lincsoft.entity.oa;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lincsoft.common.VersionedEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee entity.
 *
 * <p>Employee profile. Links to {@code mst_user} (login account, nullable), {@code mst_department},
 * {@code mst_position}, and self-references {@code managerId} (direct supervisor), which is used by
 * the approval workflow for hierarchical routing.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("mst_employee")
public class MstEmployee extends VersionedEntity {
  /** Login account ID (mst_user.id), nullable */
  private Long userId;

  /** Employee number (business identifier) */
  private String employeeNo;

  /** Real name */
  private String realName;

  /** Department ID (mst_department.id) */
  private Long deptId;

  /** Position ID (mst_position.id) */
  private Long positionId;

  /** Direct supervisor (mst_employee.id) */
  private Long managerId;

  /** Mobile phone */
  private String mobile;

  /** Email address */
  private String email;

  /** Gender (0 unknown / 1 male / 2 female) */
  private String gender;

  /** ID card number */
  private String idCardNo;

  /** Date of birth */
  private LocalDate birthday;

  /** Hire date */
  private LocalDate hireDate;

  /** Employment status (0 left / 1 active / 2 on leave) */
  private String status;
}
