package com.lincsoft.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lincsoft.constant.CommonConstants;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * MyBatis-Plus metaobject automatic field setting handler.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Slf4j
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

  @Resource private AppProperties appProperties;

  /**
   * Automatic field setting during INSERT operation.
   *
   * <p>Automatically set the following fields during INSERT operation:
   *
   * <ul>
   *   <li>{@code createBy} - Create User
   *   <li>{@code createAt} - Create Time (current timestamp)
   *   <li>{@code updateBy} - Update User
   *   <li>{@code updateAt} - Update Time (current timestamp)
   * </ul>
   *
   * @param metaObject The metaobject provided by MyBatis-Plus
   */
  @Override
  public void insertFill(MetaObject metaObject) {
    log.debug(
        "Execute automatic field configuration for INSERT operations.: {}",
        metaObject.getOriginalObject().getClass().getSimpleName());

    // current username
    String currentUser = getCurrentUser();

    // current timestamp
    LocalDateTime currentTimestamp = getCurrentTimestamp();

    // create user and create time
    this.strictInsertFill(metaObject, "createBy", String.class, currentUser);
    this.strictInsertFill(metaObject, "createAt", LocalDateTime.class, currentTimestamp);

    // update user and update time
    this.strictInsertFill(metaObject, "updateBy", String.class, currentUser);
    this.strictInsertFill(metaObject, "updateAt", LocalDateTime.class, currentTimestamp);
  }

  /**
   * Automatic field setting during UPDATE operation.
   *
   * <p>Automatically set the following fields during UPDATE operation:
   *
   * <ul>
   *   <li>{@code updateBy} - Update User
   *   <li>{@code updateAt} - Update Time (current timestamp)
   * </ul>
   *
   * @param metaObject The metaobject provided by MyBatis-Plus
   */
  @Override
  public void updateFill(MetaObject metaObject) {
    log.debug(
        "Execute automatic field configuration for UPDATE operations.: {}",
        metaObject.getOriginalObject().getClass().getSimpleName());

    // current username
    String currentUser = getCurrentUser();

    // current timestamp
    LocalDateTime currentTimestamp = getCurrentTimestamp();

    // update user and update time
    this.strictUpdateFill(metaObject, "updateBy", String.class, currentUser);
    this.strictUpdateFill(metaObject, "updateAt", LocalDateTime.class, currentTimestamp);
  }

  /**
   * Obtain the current operator username from MDC.
   *
   * @return current operator username
   */
  private String getCurrentUser() {
    String username = MDC.get(CommonConstants.MDC_CURRENT_USER_KEY);
    if (username == null || username.isBlank()) {
      return CommonConstants.MDC_DEFAULT_USERNAME;
    }
    return username;
  }

  /**
   * Get the current time.
   *
   * <p>If a web request context exists, cache the current time generated at the start of the
   * request to ensure the same timestamp is applied when multiple tables are updated within the
   * same request.
   *
   * @return current timestamp
   */
  private LocalDateTime getCurrentTimestamp() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      Object timestamp =
          attributes.getAttribute(
              CommonConstants.MDC_REQUEST_TIMESTAMP_KEY, RequestAttributes.SCOPE_REQUEST);
      if (timestamp == null) {
        timestamp = LocalDateTime.now();
        attributes.setAttribute(
            CommonConstants.MDC_REQUEST_TIMESTAMP_KEY, timestamp, RequestAttributes.SCOPE_REQUEST);
      }
      return (LocalDateTime) timestamp;
    }
    return LocalDateTime.now();
  }
}
