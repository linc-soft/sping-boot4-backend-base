package com.lincsoft.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Result class
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@DisplayName("Result Unified Response Class Tests")
class ResultTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Nested
  @DisplayName("success() Method Tests")
  class SuccessWithoutParamsTest {

    @Test
    @DisplayName("Should return code 200 with null message and data")
    void success_shouldReturnCode200WithNullMessageAndData() {
      // When
      Result<Void> result = Result.success();

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isNull();
      assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("JSON serialization should exclude null fields")
    void success_jsonSerialization_shouldExcludeNullFields() throws JsonProcessingException {
      // Given
      Result<Void> result = Result.success();

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).contains("\"code\":200");
      assertThat(json).doesNotContain("message");
      assertThat(json).doesNotContain("data");
    }
  }

  @Nested
  @DisplayName("success(String message) Method Tests")
  class SuccessWithMessageTest {

    @Test
    @DisplayName("Should return code 200 with specified message")
    void successWithMessage_shouldReturnCode200AndMessage() {
      // When
      Result<Void> result = Result.success("Operation completed");

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isEqualTo("Operation completed");
      assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("JSON serialization should include message but exclude data")
    void successWithMessage_jsonSerialization_shouldIncludeMessageExcludeData()
        throws JsonProcessingException {
      // Given
      Result<Void> result = Result.success("Operation completed");

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).contains("\"code\":200");
      assertThat(json).contains("\"message\":\"Operation completed\"");
      assertThat(json).doesNotContain("data");
    }

    @Test
    @DisplayName("Should handle empty string message")
    void successWithEmptyMessage_shouldHandleEmptyString() {
      // When
      Result<Void> result = Result.success("");

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isEmpty();
      assertThat(result.getData()).isNull();
    }
  }

  @Nested
  @DisplayName("success(T data) Method Tests")
  class SuccessWithDataTest {

    @Test
    @DisplayName("Should return code 200 with null message and data")
    void successWithData_shouldReturnCode200WithNullMessageAndData() {
      // Given
      Map<String, String> testData = Map.of("key", "value");

      // When
      Result<Map<String, String>> result = Result.success(testData);

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isNull();
      assertThat(result.getData()).isEqualTo(testData);
    }

    @Test
    @DisplayName("JSON serialization should exclude message but include data")
    void successWithData_jsonSerialization_shouldExcludeMessageIncludeData()
        throws JsonProcessingException {
      // Given
      Result<Map<String, String>> result = Result.success(Map.of("key", "value"));

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).contains("\"code\":200");
      // message 是 null，应该被排除
      assertThat(json).doesNotContain("message");
      // data 应该被包含
      assertThat(json).contains("\"data\"");
      assertThat(json).contains("\"key\":\"value\"");
    }
  }

  @Nested
  @DisplayName("success(String message, T data) Method Tests")
  class SuccessWithMessageAndDataTest {

    @Test
    @DisplayName("Should return all fields with message and data")
    void successWithMessageAndData_shouldReturnAllFields() {
      // Given
      Map<String, Integer> testData = Map.of("count", 10);

      // When
      Result<Map<String, Integer>> result = Result.success("Data retrieved", testData);

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isEqualTo("Data retrieved");
      assertThat(result.getData()).isEqualTo(testData);
    }

    @Test
    @DisplayName("JSON serialization should include all fields")
    void successWithMessageAndData_jsonSerialization_shouldIncludeAllFields()
        throws JsonProcessingException {
      // Given
      Result<String> result = Result.success("Custom message", "test data");

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).contains("\"code\":200");
      assertThat(json).contains("\"message\":\"Custom message\"");
      assertThat(json).contains("\"data\":\"test data\"");
    }
  }

  @Nested
  @DisplayName("error(int code) Method Tests")
  class ErrorWithCodeOnlyTest {

    @Test
    @DisplayName("Should return specified code with null message and data")
    void errorWithCodeOnly_shouldReturnCodeWithNullMessageAndData() {
      // When
      Result<Void> result = Result.error(400);

      // Then
      assertThat(result.getCode()).isEqualTo(400);
      assertThat(result.getMessage()).isNull();
      assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("JSON serialization should only include code field")
    void errorWithCodeOnly_jsonSerialization_shouldOnlyIncludeCode()
        throws JsonProcessingException {
      // Given
      Result<Void> result = Result.error(500);

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).isEqualTo("{\"code\":500}");
      assertThat(json).doesNotContain("message");
      assertThat(json).doesNotContain("data");
    }
  }

  @Nested
  @DisplayName("error(int code, String message) Method Tests")
  class ErrorWithCodeAndMessageTest {

    @Test
    @DisplayName("Should return specified code and message with null data")
    void errorWithCodeAndMessage_shouldReturnCodeAndMessageWithNullData() {
      // When
      Result<Void> result = Result.error(400, "Bad request");

      // Then
      assertThat(result.getCode()).isEqualTo(400);
      assertThat(result.getMessage()).isEqualTo("Bad request");
      assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("JSON serialization should exclude data field")
    void errorWithCodeAndMessage_jsonSerialization_shouldExcludeDataField()
        throws JsonProcessingException {
      // Given
      Result<Void> result = Result.error(500, "Internal server error");

      // When
      String json = objectMapper.writeValueAsString(result);

      // Then
      assertThat(json).contains("\"code\":500");
      assertThat(json).contains("\"message\":\"Internal server error\"");
      assertThat(json).doesNotContain("data");
    }
  }

  @Nested
  @DisplayName("Generic Type Tests")
  class GenericTypeTest {

    @Test
    @DisplayName("Should handle Integer type data")
    void generic_withIntegerData() {
      // When
      Result<Integer> result = Result.success(42);

      // Then
      assertThat(result.getCode()).isEqualTo(200);
      assertThat(result.getMessage()).isNull();
      assertThat(result.getData()).isInstanceOf(Integer.class);
      assertThat(result.getData()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should handle Map type data")
    void generic_withMapData() {
      // Given
      Map<String, Object> data = Map.of("id", 1, "name", "Test");

      // When
      Result<Map<String, Object>> result = Result.success(data);

      // Then
      assertThat(result.getData()).isInstanceOf(Map.class);
      assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle custom object type data")
    void generic_withCustomObjectData() {
      // Given
      TestData customData = new TestData("test-id", "Test Name");

      // When
      Result<TestData> result = Result.success(customData);

      // Then
      assertThat(result.getData()).isInstanceOf(TestData.class);
      assertThat(result.getData().id()).isEqualTo("test-id");
      assertThat(result.getData().name()).isEqualTo("Test Name");
    }

    /** Simple test data record */
    record TestData(String id, String name) {}
  }
}
