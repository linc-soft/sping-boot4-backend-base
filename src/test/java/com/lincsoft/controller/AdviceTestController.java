package com.lincsoft.controller;

import com.lincsoft.advice.GlobalResponseAdvice;
import com.lincsoft.annotation.IgnoreResultWrapper;
import com.lincsoft.common.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for integration testing of {@link GlobalResponseAdvice}
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/test/advice")
public class AdviceTestController {

  @GetMapping("/normal")
  public String normalString() {
    return "success";
  }

  @GetMapping("/object")
  public DummyUser normalObject() {
    return new DummyUser("Alice", 25);
  }

  @GetMapping("/null")
  public Object nullValue() {
    return null;
  }

  @GetMapping("/result")
  public Result<String> existingResult() {
    return Result.success("already wrapped");
  }

  @GetMapping("/ignore")
  @IgnoreResultWrapper
  public String ignoredMethod() {
    return "ignored string";
  }

  // --- Dummy class for testing object serialization ---
  @Data
  @AllArgsConstructor
  public static class DummyUser {
    private String name;
    private int age;
  }
}
