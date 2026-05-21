package com.lincsoft.exception;

import com.lincsoft.constant.MessageEnums;
import java.io.Serial;
import lombok.Getter;

/**
 * Business Exception class, used to throw business exceptions.
 *
 * <p>Store messageKey and formatting parameters, construct Result by GlobalExceptionHandler, and
 * finally parse it into the corresponding message text by GlobalResponseAdvice based on the current
 * language.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public class BusinessException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  @Getter private final int code;

  @Getter private final String messageKey;

  @Getter private final Object[] messageArgs;

  /**
   * Constructor used to create business exceptions with a plain message.
   *
   * @param message Exception message
   */
  public BusinessException(String message) {
    super(message);
    this.code = MessageEnums.FAIL.getCode();
    this.messageKey = null;
    this.messageArgs = null;
  }

  /**
   * Constructor used to create business exceptions with MessageEnums and optional format arguments.
   *
   * @param messageEnum Exception Enumeration
   * @param args Format arguments for the message
   */
  public BusinessException(MessageEnums messageEnum, Object... args) {
    super(messageEnum.getMessageKey());
    this.code = messageEnum.getCode();
    this.messageKey = messageEnum.getMessageKey();
    this.messageArgs = args;
  }
}
