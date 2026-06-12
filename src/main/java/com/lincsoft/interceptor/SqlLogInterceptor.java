package com.lincsoft.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lincsoft.config.AppProperties;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.entity.system.SysSqlLog;
import com.lincsoft.services.system.SqlLogService;
import com.lincsoft.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@RequiredArgsConstructor
@Intercepts({
  @Signature(
      type = Executor.class,
      method = "query",
      args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(
      type = Executor.class,
      method = "update",
      args = {MappedStatement.class, Object.class})
})
public class SqlLogInterceptor implements Interceptor {

  private static final String SYS_SQL_LOG_MAPPER = "SysSqlLogMapper";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final SqlLogService sqlLogService;
  private final AppProperties appProperties;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    MappedStatement mappedStatement = resolveMappedStatement(invocation);
    if (mappedStatement == null || shouldSkip(mappedStatement)) {
      return invocation.proceed();
    }

    long startTime = System.currentTimeMillis();
    Object result = invocation.proceed();
    long duration = System.currentTimeMillis() - startTime;

    try {
      saveSqlLog(invocation, mappedStatement, result, duration);
    } catch (Exception e) {
      log.error("Error occurred while recording SQL log: {}", e.getMessage(), e);
    }

    return result;
  }

  private MappedStatement resolveMappedStatement(Invocation invocation) {
    try {
      Object[] args = invocation.getArgs();
      if (args.length > 0 && args[0] instanceof MappedStatement mappedStatement) {
        return mappedStatement;
      }
    } catch (Exception e) {
      log.error("Failed to resolve mapped statement for SQL logging: {}", e.getMessage(), e);
    }
    return null;
  }

  private boolean shouldSkip(MappedStatement mappedStatement) {
    AppProperties.SqlLog sqlLogProperties = appProperties.getSqlLog();
    if (sqlLogProperties == null || !sqlLogProperties.isEnabled()) {
      return true;
    }

    String statementId = mappedStatement.getId();
    if (statementId != null && statementId.contains(SYS_SQL_LOG_MAPPER)) {
      return true;
    }

    String mapperClass = extractMapperClass(statementId);
    String simpleMapperClass = extractSimpleClassName(mapperClass);
    List<String> excludedMapperClasses = sqlLogProperties.getExcludeMapperClasses();
    if (excludedMapperClasses != null && !excludedMapperClasses.isEmpty()) {
      boolean excluded =
          excludedMapperClasses.stream()
              .filter(
                  excludedMapperClass ->
                      excludedMapperClass != null && !excludedMapperClass.isBlank())
              .map(String::trim)
              .anyMatch(
                  excludedMapperClass ->
                      excludedMapperClass.equals(mapperClass)
                          || excludedMapperClass.equals(simpleMapperClass));
      if (excluded) {
        return true;
      }
    }

    return !matchesIncludePathPatterns(sqlLogProperties.getIncludePathPatterns());
  }

  private boolean matchesIncludePathPatterns(List<String> includePathPatterns) {
    if (CollectionUtils.isEmpty(includePathPatterns)) {
      return true;
    }

    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
      return false;
    }

    String requestUri = servletRequestAttributes.getRequest().getRequestURI();
    return includePathPatterns.stream()
        .filter(pattern -> pattern != null && !pattern.isBlank())
        .map(String::trim)
        .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
  }

  private void saveSqlLog(
      Invocation invocation, MappedStatement mappedStatement, Object result, long duration) {
    Object parameterObject = invocation.getArgs()[1];
    BoundSql boundSql = mappedStatement.getBoundSql(parameterObject);
    String statementId = mappedStatement.getId();

    SysSqlLog sqlLog = new SysSqlLog();
    sqlLog.setTraceId(MDC.get(CommonConstants.MDC_TRACE_ID_KEY));
    sqlLog.setSqlText(buildSqlText(mappedStatement, boundSql));
    sqlLog.setSqlParams(buildSqlParams(mappedStatement, boundSql));
    sqlLog.setDuration(duration);
    sqlLog.setMapperClass(extractMapperClass(statementId));
    sqlLog.setMapperMethod(extractMapperMethod(statementId));
    sqlLog.setSqlType(mappedStatement.getSqlCommandType().name());
    sqlLog.setUsername(MDC.get(CommonConstants.MDC_CURRENT_USER_KEY));
    sqlLog.setRowCount(resolveRowCount(mappedStatement.getSqlCommandType(), result));
    sqlLog.setCreateTime(LocalDateTime.now());
    populateRequestInfo(sqlLog);

    sqlLogService.save(sqlLog);
  }

  private String buildSqlText(MappedStatement mappedStatement, BoundSql boundSql) {
    String sql = normalizeSql(boundSql.getSql());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (sql == null || parameterMappings == null || parameterMappings.isEmpty()) {
      return LogUtil.truncate(sql, CommonConstants.MAX_TEXT_LENGTH);
    }

    TypeHandlerRegistry typeHandlerRegistry =
        mappedStatement.getConfiguration().getTypeHandlerRegistry();
    Object parameterObject = boundSql.getParameterObject();
    MetaObject metaObject =
        parameterObject == null
            ? null
            : mappedStatement.getConfiguration().newMetaObject(parameterObject);

    String resolvedSql = sql;
    for (ParameterMapping parameterMapping : parameterMappings) {
      if (parameterMapping.getMode() == ParameterMode.OUT) {
        continue;
      }
      Object value =
          resolveParameterValue(
              parameterMapping.getProperty(),
              boundSql,
              parameterObject,
              metaObject,
              typeHandlerRegistry);
      resolvedSql =
          resolvedSql.replaceFirst("\\?", Matcher.quoteReplacement(formatSqlValue(value)));
    }

    return LogUtil.truncate(resolvedSql, CommonConstants.MAX_TEXT_LENGTH);
  }

  private String buildSqlParams(MappedStatement mappedStatement, BoundSql boundSql) {
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings == null || parameterMappings.isEmpty()) {
      return null;
    }

    TypeHandlerRegistry typeHandlerRegistry =
        mappedStatement.getConfiguration().getTypeHandlerRegistry();
    Object parameterObject = boundSql.getParameterObject();
    MetaObject metaObject =
        parameterObject == null
            ? null
            : mappedStatement.getConfiguration().newMetaObject(parameterObject);
    Map<String, Object> params = new LinkedHashMap<>();

    for (ParameterMapping parameterMapping : parameterMappings) {
      if (parameterMapping.getMode() == ParameterMode.OUT) {
        continue;
      }
      String property = parameterMapping.getProperty();
      params.put(
          property,
          resolveParameterValue(
              property, boundSql, parameterObject, metaObject, typeHandlerRegistry));
    }

    try {
      return LogUtil.truncate(
          LogUtil.sanitizeBody(OBJECT_MAPPER.writeValueAsString(params)),
          CommonConstants.MAX_TEXT_LENGTH);
    } catch (Exception e) {
      log.debug("Failed to serialize SQL parameters: {}", e.getMessage());
      return LogUtil.truncate(
          LogUtil.sanitizeBody(String.valueOf(params)), CommonConstants.MAX_TEXT_LENGTH);
    }
  }

  private Object resolveParameterValue(
      String property,
      BoundSql boundSql,
      Object parameterObject,
      MetaObject metaObject,
      TypeHandlerRegistry typeHandlerRegistry) {
    if (boundSql.hasAdditionalParameter(property)) {
      return boundSql.getAdditionalParameter(property);
    }
    if (parameterObject == null) {
      return null;
    }
    if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
      return parameterObject;
    }
    if (parameterObject instanceof Map<?, ?> parameterMap && parameterMap.containsKey(property)) {
      return parameterMap.get(property);
    }
    if (metaObject != null && metaObject.hasGetter(property)) {
      return metaObject.getValue(property);
    }
    return null;
  }

  private String formatSqlValue(Object value) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof Number || value instanceof Boolean) {
      return value.toString();
    }
    if (value instanceof byte[] bytes) {
      return "'<binary:" + bytes.length + ">'";
    }

    String text = LogUtil.sanitizeBody(String.valueOf(value));
    if (value instanceof Enum<?> enumValue) {
      text = enumValue.name();
    } else if (value instanceof Date || value instanceof TemporalAccessor) {
      text = value.toString();
    }
    return "'" + text.replace("'", "''") + "'";
  }

  private Long resolveRowCount(SqlCommandType sqlCommandType, Object result) {
    if (sqlCommandType == SqlCommandType.SELECT || !(result instanceof Number rowCount)) {
      return null;
    }
    return rowCount.longValue();
  }

  private void populateRequestInfo(SysSqlLog sqlLog) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (!(requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)) {
      return;
    }

    HttpServletRequest request = servletRequestAttributes.getRequest();
    sqlLog.setRequestUrl(request.getRequestURI());
    sqlLog.setRequestMethod(request.getMethod());
    sqlLog.setClientIp(LogUtil.getClientIp(request));
  }

  private String normalizeSql(String sql) {
    if (sql == null) {
      return null;
    }
    return sql.replaceAll("\\s+", " ").trim();
  }

  private String extractMapperClass(String statementId) {
    if (statementId == null || statementId.isBlank()) {
      return null;
    }
    int lastDotIndex = statementId.lastIndexOf('.');
    if (lastDotIndex < 0) {
      return statementId;
    }
    return statementId.substring(0, lastDotIndex);
  }

  private String extractMapperMethod(String statementId) {
    if (statementId == null || statementId.isBlank()) {
      return null;
    }
    int lastDotIndex = statementId.lastIndexOf('.');
    if (lastDotIndex < 0 || lastDotIndex == statementId.length() - 1) {
      return statementId;
    }
    return statementId.substring(lastDotIndex + 1);
  }

  private String extractSimpleClassName(String className) {
    if (className == null || className.isBlank()) {
      return className;
    }
    int lastDotIndex = className.lastIndexOf('.');
    if (lastDotIndex < 0 || lastDotIndex == className.length() - 1) {
      return className;
    }
    return className.substring(lastDotIndex + 1);
  }
}
