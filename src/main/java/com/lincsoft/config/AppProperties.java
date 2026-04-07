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
public class AppProperties {}
