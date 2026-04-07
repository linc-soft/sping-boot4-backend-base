package com.lincsoft.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration class.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Configuration
@MapperScan("com.lincsoft.mapper")
public class MyBatisPlusConfig {}
