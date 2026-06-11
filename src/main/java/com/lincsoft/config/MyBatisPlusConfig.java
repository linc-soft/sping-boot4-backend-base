package com.lincsoft.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.lincsoft.interceptor.SqlLogInterceptor;
import com.lincsoft.services.system.SqlLogService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

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
    // Add pagination plugin
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // Add optimistic locking plugin
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }

  /**
   * SQL Log Interceptor
   *
   * <p>Registers the SQL log interceptor as a MyBatis plugin. This is a native MyBatis Interceptor
   * (not an InnerInterceptor), so it must be registered as a standalone bean rather than added to
   * MybatisPlusInterceptor.
   *
   * @param sqlLogService the SQL log service
   * @param appProperties the application properties
   * @return SqlLogInterceptor
   */
  @Bean
  public SqlLogInterceptor sqlLogInterceptor(
      @Lazy SqlLogService sqlLogService, AppProperties appProperties) {
    return new SqlLogInterceptor(sqlLogService, appProperties);
  }
}
