package com.lincsoft.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties class.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private Mdc mdc;

  /**
   * MDC properties class.
   *
   * @author 林创科技
   * @since 2026-04-07
   */
  @Data
  public static class Mdc {
    /** Key for current user in MDC */
    private String currentUserKey = "currentUser";

    /** Key for request timestamp in MDC */
    private String requestTimestampKey = "requestTimestamp";
  }
}
