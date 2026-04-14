package com.lincsoft.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus configuration class.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Configuration
@MapperScan("com.lincsoft.mapper")
public class MyBatisPlusConfig {

  /**
   * MyBatis-Plus 插件注册。
   *
   * <p>注册乐观锁插件，更新时自动校验并递增 version 字段。
   *
   * @return MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }
}
