package com.lincsoft.controller.master.vo;

import com.lincsoft.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User page query request VO.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserPageRequest extends PageRequest {
  /** Username */
  private String username;

  /** Status */
  private String status;
}
