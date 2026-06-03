package com.lincsoft.controller.master.vo;

import com.lincsoft.common.PageRequest;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Employee page query request VO.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EmployeePageRequest extends PageRequest {

  /** Username or nickname filter */
  private String keyword;

  /** Status filter */
  private String status;

  public Set<String> allowedSortColumns() {
    return Set.of("username", "nickname", "status", "create_at");
  }
}
