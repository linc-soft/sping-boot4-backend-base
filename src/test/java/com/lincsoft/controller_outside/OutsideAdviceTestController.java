package com.lincsoft.controller_outside;

import com.lincsoft.advice.GlobalResponseAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for integration testing of {@link GlobalResponseAdvice}
 *
 * <p>Controllers outside "com.lincsoft.controller" will not be intercepted by {@link
 * GlobalResponseAdvice}.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/outside/advice")
public class OutsideAdviceTestController {

  @GetMapping("/normal")
  public String normalString() {
    return "outside";
  }
}
