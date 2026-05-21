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
 * @param <T> data type
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** status code */
  private Integer code;

  /** Message key */
  private String messageKey;

  /** Format parameters */
  private Object[] messageArgs;

  /** message text */
  private String message;

  /** data */
  private T data;

  // ========== Static factory method ==========

  /** Successful response (no data) */
  public static <T> Result<T> success() {
    return new Result<>(MessageEnums.SUCCESS.getCode(), null, null, null, null);
  }

  /** Successful response (with data) */
  public static <T> Result<T> success(T data) {
    return new Result<>(MessageEnums.SUCCESS.getCode(), null, null, null, data);
  }

  /** Error response (with status code, message key, and formatting parameters) */
  public static <T> Result<T> error(int code, String messageKey, Object... args) {
    return new Result<>(code, messageKey, args, null, null);
  }

  /** Error response (based on Messages) */
  public static <T> Result<T> error(MessageEnums messageEnum) {
    return new Result<>(messageEnum.getCode(), messageEnum.getMessageKey(), null, null, null);
  }
}
