package com.lincsoft.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Message enumeration class, used to unify the codes and messages of returned results.
 *
 * <p>Success: 200
 *
 * <p>Fail: 500
 *
 * <p>HTTP Status: Error codes and error messages are added with reference to the <a
 * href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">HTTP Status Codes</a>.
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@AllArgsConstructor
public enum MessageEnums {
  SUCCESS(200, null),
  FAIL(500, null),
  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  INTERNAL_SERVER_ERROR(500, "Internal Server Error");

  @Getter private final int code;
  @Getter private final String message;
}
