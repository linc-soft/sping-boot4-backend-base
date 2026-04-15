package com.lincsoft.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.lincsoft.interceptor.DataPermissionInterceptor;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class MyBatisPlusConfig {

  private final DataPermissionInterceptor dataPermissionInterceptor;

  /**
   * MyBatis-Plus Plugin Registration
   *
   * <p>Register the pagination plugin and the optimistic locking plugin.
   *
   * <p>Note: The pagination plugin must be added before the optimistic locking plugin.
   *
   * @return MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // Add data permission interceptor first (must run before pagination)
    interceptor.addInnerInterceptor(dataPermissionInterceptor);
    // Add pagination plugin
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // Add optimistic locking plugin
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }
}
