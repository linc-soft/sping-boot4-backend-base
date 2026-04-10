package com.lincsoft.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lincsoft.controller.AdviceTestController;
import com.lincsoft.controller.HttpEntityAdviceTestController;
import com.lincsoft.controller.IgnoredClassAdviceTestController;
import com.lincsoft.controller_outside.OutsideAdviceTestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for {@link GlobalResponseAdvice}
 *
 * <p>In a Spring MVC environment, verify that {@link GlobalResponseAdvice} correctly intercepts and
 * wraps the controller responses.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@WebMvcTest(
    controllers = {
      AdviceTestController.class,
      OutsideAdviceTestController.class,
      IgnoredClassAdviceTestController.class,
      HttpEntityAdviceTestController.class
    },
    excludeFilters =
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.lincsoft\\.(config|interceptor|filter|exception|services)\\..*"))
@Import(GlobalResponseAdvice.class)
class GlobalResponseAdviceIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldWrapNormalStringResponse() throws Exception {
    mockMvc
        .perform(get("/test/advice/normal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").value("success"));
  }

  @Test
  void shouldWrapObjectResponse() throws Exception {
    mockMvc
        .perform(get("/test/advice/object"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.name").value("Alice"))
        .andExpect(jsonPath("$.data.age").value(25));
  }

  @Test
  void shouldWrapNullResponse() throws Exception {
    mockMvc
        .perform(get("/test/advice/null"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void shouldNotWrapExistingResultResponse() throws Exception {
    // If the return value is already of the Result type, it will not be
    // double-wrapped.Result.success("already wrapped") will be set to the message field.
    mockMvc
        .perform(get("/test/advice/result"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.message").value("already wrapped"));
  }

  @Test
  void shouldNotWrapIgnoredMethodResponse() throws Exception {
    mockMvc
        .perform(get("/test/advice/ignore"))
        .andExpect(status().isOk())
        .andExpect(content().string("ignored string"));
  }

  @Test
  void shouldNotWrapControllersOutsideBasePackages() throws Exception {
    // Since basePackages = "com.lincsoft.controller", controllers in the
    // "com.lincsoft.controller_outside" package will not be intercepted.
    mockMvc
        .perform(get("/outside/advice/normal"))
        .andExpect(status().isOk())
        .andExpect(content().string("outside"));
  }

  @Test
  void shouldNotWrapClassLevelIgnoredController() throws Exception {
    // Controllers annotated with @IgnoreResultWrapper at the class level will not be wrapped.
    mockMvc
        .perform(get("/test/advice/ignored-class/normal"))
        .andExpect(status().isOk())
        .andExpect(content().string("not wrapped"));
  }

  @Test
  void shouldNotWrapHttpEntityResponse() throws Exception {
    // If the return type is HttpEntity (ResponseEntity), it will not be wrapped.
    mockMvc
        .perform(get("/test/advice/http-entity/response-entity"))
        .andExpect(status().isOk())
        .andExpect(content().string("raw response entity"));
  }
}
