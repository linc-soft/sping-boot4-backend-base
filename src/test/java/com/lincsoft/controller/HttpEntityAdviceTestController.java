package com.lincsoft.controller;

import com.lincsoft.advice.GlobalResponseAdvice;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for integration testing of {@link GlobalResponseAdvice}
 *
 * <p>Test controller that returns {@link HttpEntity}. Return values of type {@link HttpEntity} are
 * excluded from response wrapping.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/test/advice/http-entity")
public class HttpEntityAdviceTestController {

  /** Endpoint that returns {@link ResponseEntity}.Verify that it is not wrapped. */
  @GetMapping("/response-entity")
  public ResponseEntity<String> responseEntity() {
    return ResponseEntity.ok("raw response entity");
  }
}
