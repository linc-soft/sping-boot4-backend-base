package com.lincsoft.exception;

import com.lincsoft.constant.MessageEnums;
import java.io.Serial;
import lombok.Getter;

/**
 * Business Exception class, used to throw business exceptions.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class BusinessException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  @Getter private final int code;

  /**
   * Constructor used to create business exceptions.
   *
   * @param message Exception message
   */
  public BusinessException(String message) {
    super(message);
    this.code = MessageEnums.FAIL.getCode();
  }

  /**
   * Constructor used to create business exceptions.
   *
   * @param message Exception Enumeration
   */
  public BusinessException(MessageEnums message, Object... args) {
    super(MessageEnums.format(message, args));
    this.code = message.getCode();
  }
}
