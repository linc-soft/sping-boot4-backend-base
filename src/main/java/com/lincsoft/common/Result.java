package com.lincsoft.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lincsoft.constant.MessageEnums;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result class for unified response format
 *
 * @param <T>
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
  /* Status code */
  private Integer code;
  /* Status message */
  private String message;
  /* Data */
  private T data;

  /* Success */
  public static <T> Result<T> success() {
    return new Result<>(MessageEnums.SUCCESS.getCode(), null, null);
  }

  /* Success with message */
  public static <T> Result<T> success(String message) {
    return new Result<>(MessageEnums.SUCCESS.getCode(), message, null);
  }

  /* Success with data */
  public static <T> Result<T> success(T data) {
    return new Result<>(MessageEnums.SUCCESS.getCode(), null, data);
  }

  /* Success with message and data */
  public static <T> Result<T> success(String message, T data) {
    return new Result<>(MessageEnums.SUCCESS.getCode(), message, data);
  }

  /* Error with code */
  public static <T> Result<T> error(int code) {
    return new Result<>(code, null, null);
  }

  public static <T> Result<T> error(int code, String message) {
    return new Result<>(code, message, null);
  }

  /* Error with code and message */
  public static <T> Result<T> error(MessageEnums message) {
    return new Result<>(message.getCode(), message.getMessage(), null);
  }
}
