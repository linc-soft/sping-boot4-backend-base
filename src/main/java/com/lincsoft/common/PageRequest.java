package com.lincsoft.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Page request base class.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Data
public abstract class PageRequest {
  /** Page number */
  @Min(value = 1, message = "Page number must be greater than or equal to 1")
  private int page;

  /** Page size */
  @Min(value = 10, message = "Page size must be greater than or equal to 10")
  @Max(value = 100, message = "Page size must be less than or equal to 100")
  private int size;
}
