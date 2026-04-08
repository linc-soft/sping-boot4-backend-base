package com.lincsoft.controller;

import com.lincsoft.advice.GlobalResponseAdvice;
import com.lincsoft.annotation.IgnoreResultWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for integration testing of {@link GlobalResponseAdvice}
 *
 * <p>Test controller annotated with {@link IgnoreResultWrapper} at the class level.All methods of
 * this controller are excluded from response wrapping.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/test/advice/ignored-class")
@IgnoreResultWrapper
public class IgnoredClassAdviceTestController {

  /**
   * Returns a string that will not be wrapped due to the class-level {@link IgnoreResultWrapper}.
   */
  @GetMapping("/normal")
  public String normalString() {
    return "not wrapped";
  }
}
