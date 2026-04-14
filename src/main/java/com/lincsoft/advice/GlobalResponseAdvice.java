package com.lincsoft.advice;

import com.lincsoft.annotation.IgnoreResultWrapper;
import com.lincsoft.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Global Response Handler
 *
 * <p>An advice class that uniformly wraps the return values of controllers.It automatically wraps
 * all successful responses with the {@link Result} class and returns them. For return values of
 * type String, they are converted to a JSON string before being returned.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.lincsoft.controller")
@RequiredArgsConstructor
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    // If @IgnoreResultWrapper is applied at the class level, the result will not be wrapped.
    if (returnType.getDeclaringClass().isAnnotationPresent(IgnoreResultWrapper.class)) {
      return false;
    }
    // If @IgnoreResultWrapper is applied at the method level, the result will not be wrapped.
    if (returnType.hasMethodAnnotation(IgnoreResultWrapper.class)) {
      return false;
    }
    // If the return value is already of type Result, it will not be wrapped.
    if (Result.class.isAssignableFrom(returnType.getParameterType())) {
      return false;
    }
    // If the return type is HttpEntity used by Spring WebFlux, it will not be wrapped.
    if (org.springframework.http.HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
      return false;
    }
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      @NonNull MethodParameter returnType,
      @NonNull MediaType selectedContentType,
      @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response) {
    // If the return value is already of type Result, it will not be wrapped.
    if (body instanceof Result) {
      return body;
    }

    /*
     StringHttpMessageConverter case: Controller returns String type. Spring selects
     StringHttpMessageConverter for String return types, which would output plain text instead of
     JSON. Here we manually serialize the wrapped Result to JSON string and set the content type
     to application/json to ensure consistent API response format. Example: return "hello" →
     {"code": 200, "data": "hello"}
    */
    if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
      try {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return objectMapper.writeValueAsString(Result.success(body));
      } catch (JacksonException e) {
        log.error("JSON serialization failed.", e);
        throw new RuntimeException("JSON serialization failed.", e);
      }
    }
    // If the return value is null, it will be wrapped with Result.success(null).
    return Result.success(body);
  }
}
