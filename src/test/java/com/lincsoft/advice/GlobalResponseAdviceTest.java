package com.lincsoft.advice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lincsoft.annotation.IgnoreResultWrapper;
import com.lincsoft.common.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit test for {@link GlobalResponseAdvice}
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class GlobalResponseAdviceTest {

  @Mock private ObjectMapper objectMapper;

  @InjectMocks private GlobalResponseAdvice globalResponseAdvice;

  @Mock private MethodParameter methodParameter;
  @Mock private ServerHttpRequest request;
  @Mock private ServerHttpResponse response;

  // --- Dummy Classes for Testing Annotations ---

  @IgnoreResultWrapper
  private static class IgnoredClass {}

  private static class NormalClass {}

  // ===================================================================================
  // Tests for supports() method
  // ===================================================================================

  @Test
  void supports_ShouldReturnFalse_WhenClassHasIgnoreAnnotation() {
    when(methodParameter.getDeclaringClass()).thenReturn((Class) IgnoredClass.class);

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    boolean supports = globalResponseAdvice.supports(methodParameter, converterType);

    assertFalse(supports);
  }

  @Test
  void supports_ShouldReturnFalse_WhenMethodHasIgnoreAnnotation() {
    when(methodParameter.getDeclaringClass()).thenReturn((Class) NormalClass.class);
    when(methodParameter.hasMethodAnnotation(IgnoreResultWrapper.class)).thenReturn(true);

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    boolean supports = globalResponseAdvice.supports(methodParameter, converterType);

    assertFalse(supports);
  }

  @Test
  void supports_ShouldReturnFalse_WhenReturnTypeIsResult() {
    when(methodParameter.getDeclaringClass()).thenReturn((Class) NormalClass.class);
    when(methodParameter.hasMethodAnnotation(IgnoreResultWrapper.class)).thenReturn(false);
    when(methodParameter.getParameterType()).thenReturn((Class) Result.class);

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    boolean supports = globalResponseAdvice.supports(methodParameter, converterType);

    assertFalse(supports);
  }

  @Test
  void supports_ShouldReturnFalse_WhenReturnTypeIsHttpEntity() {
    when(methodParameter.getDeclaringClass()).thenReturn((Class) NormalClass.class);
    when(methodParameter.hasMethodAnnotation(IgnoreResultWrapper.class)).thenReturn(false);
    when(methodParameter.getParameterType()).thenReturn((Class) HttpEntity.class);

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    boolean supports = globalResponseAdvice.supports(methodParameter, converterType);

    assertFalse(supports);
  }

  @Test
  void supports_ShouldReturnTrue_WhenNormalReturnType() {
    when(methodParameter.getDeclaringClass()).thenReturn((Class) NormalClass.class);
    when(methodParameter.hasMethodAnnotation(IgnoreResultWrapper.class)).thenReturn(false);
    when(methodParameter.getParameterType()).thenReturn((Class) String.class);

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    boolean supports = globalResponseAdvice.supports(methodParameter, converterType);

    assertTrue(supports);
  }

  // ===================================================================================
  // Tests for beforeBodyWrite() method
  // ===================================================================================

  @Test
  void beforeBodyWrite_ShouldReturnSameBody_WhenBodyIsAlreadyResult() {
    Result<String> existingResult = Result.success("Already Result");

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    Object result =
        globalResponseAdvice.beforeBodyWrite(
            existingResult,
            methodParameter,
            MediaType.APPLICATION_JSON,
            converterType,
            request,
            response);

    assertEquals(existingResult, result);
  }

  @Test
  void beforeBodyWrite_ShouldWrapWithResult_WhenBodyIsNormalObject() {
    String normalBody = "Normal Body";

    Class<? extends HttpMessageConverter<?>> converterType = (Class) HttpMessageConverter.class;
    Object result =
        globalResponseAdvice.beforeBodyWrite(
            normalBody,
            methodParameter,
            MediaType.APPLICATION_JSON,
            converterType,
            request,
            response);

    assertInstanceOf(Result.class, result);
    assertEquals(normalBody, ((Result<?>) result).getData());
    assertEquals(200, ((Result<?>) result).getCode());
  }

  @Test
  void beforeBodyWrite_ShouldSerializeToJsonAndSetContentType_WhenStringConverterIsUsed() {
    String stringBody = "String Body";
    String expectedJson = "{\"code\":200,\"data\":\"String Body\"}";
    HttpHeaders headers = new HttpHeaders();

    when(response.getHeaders()).thenReturn(headers);
    when(objectMapper.writeValueAsString(any(Result.class))).thenReturn(expectedJson);

    Class<? extends HttpMessageConverter<?>> converterType = StringHttpMessageConverter.class;
    Object result =
        globalResponseAdvice.beforeBodyWrite(
            stringBody, methodParameter, MediaType.TEXT_PLAIN, converterType, request, response);

    assertEquals(expectedJson, result);
    assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    verify(objectMapper).writeValueAsString(any(Result.class));
  }

  @Test
  void beforeBodyWrite_ShouldThrowRuntimeException_WhenJsonSerializationFails() {
    String stringBody = "String Body";
    HttpHeaders headers = new HttpHeaders();

    when(response.getHeaders()).thenReturn(headers);

    // Create a mock JacksonException, as JacksonException is an abstract class
    JacksonException jacksonException = mock(JacksonException.class);
    when(objectMapper.writeValueAsString(any(Result.class))).thenThrow(jacksonException);

    Class<? extends HttpMessageConverter<?>> converterType = StringHttpMessageConverter.class;
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                globalResponseAdvice.beforeBodyWrite(
                    stringBody,
                    methodParameter,
                    MediaType.TEXT_PLAIN,
                    converterType,
                    request,
                    response));

    assertEquals("JSON serialization failed.", exception.getMessage());
    assertEquals(jacksonException, exception.getCause());
  }
}
