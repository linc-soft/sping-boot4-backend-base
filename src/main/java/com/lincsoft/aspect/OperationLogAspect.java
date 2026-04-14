package com.lincsoft.aspect;

import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.entity.system.SysOperationLog;
import com.lincsoft.services.system.OperationLogAsyncService;
import com.lincsoft.util.LogUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that intercepts service methods annotated with {@link OperationLog} to automatically
 * capture operation metadata and persist asynchronously.
 *
 * <p>Core responsibilities:
 *
 * <ul>
 *   <li>Extracts module, subModule, type, and description from the annotation
 *   <li>Retrieves traceId from MDC for the current request
 *   <li>Calculates method execution duration (milliseconds)
 *   <li>Builds {@link SysOperationLog} entity and delegates to {@link OperationLogAsyncService} for
 *       asynchronous persistence
 *   <li>Transaction-aware: persists log after transaction commit, skips on rollback
 *   <li>Records log even on exception, then rethrows the original exception
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class OperationLogAspect {

  private final OperationLogAsyncService operationLogAsyncService;

  /**
   * Around advice: intercepts methods annotated with {@link OperationLog}.
   *
   * @param joinPoint the join point
   * @param operationLog the annotation instance
   * @return the return value of the target method
   * @throws Throwable the original exception thrown by the target method
   */
  @Around("@annotation(operationLog)")
  public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
    long startTime = System.currentTimeMillis();
    Object result = null;

    try {
      result = joinPoint.proceed();
      return result;
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      String description = resolveDescription(operationLog.description(), joinPoint, result);
      SysOperationLog logEntity = buildLogEntity(operationLog, description, duration);
      saveWithTransactionAwareness(logEntity);
    }
  }

  /**
   * Transaction-aware log persistence.
   *
   * <p>If an active transaction exists, registers a {@link TransactionSynchronization} callback:
   * persists the log asynchronously after transaction commit, skips on rollback. If no active
   * transaction, persists directly.
   *
   * @param logEntity the operation log entity
   */
  private void saveWithTransactionAwareness(SysOperationLog logEntity) {
    try {
      if (TransactionSynchronizationManager.isActualTransactionActive()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCommit() {
                operationLogAsyncService.saveOperationLog(logEntity);
              }
            });
      } else {
        operationLogAsyncService.saveOperationLog(logEntity);
      }
    } catch (Exception e) {
      log.error(
          "Exception occurred while processing operation log persistence: {}", e.getMessage(), e);
    }
  }

  private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{(.+?)}");
  private static final ExpressionParser PARSER = new SpelExpressionParser();

  /**
   * Resolves SpEL expressions in the description.
   *
   * <p>Supports expressions like {@code #{#paramName}}, {@code #{#result}}, {@code
   * #{#result.propertyName}}. Evaluated in a read-only data-binding context that only permits
   * property access (no method invocation) to prevent unintended side effects.
   *
   * <p>On resolution failure, logs a warn message and returns the original description string.
   *
   * @param descTemplate the description template string
   * @param joinPoint the join point (used to obtain method parameters)
   * @param result the method return value (maybe null)
   * @return the resolved description string
   */
  String resolveDescription(String descTemplate, ProceedingJoinPoint joinPoint, Object result) {
    if (!descTemplate.contains("#{") || !descTemplate.contains("}")) {
      return descTemplate;
    }

    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      String[] paramNames = signature.getParameterNames();
      Object[] args = joinPoint.getArgs();

      SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
      if (paramNames != null) {
        for (int i = 0; i < paramNames.length; i++) {
          context.setVariable(paramNames[i], args[i]);
        }
      }
      context.setVariable("result", result);

      Matcher matcher = SPEL_PATTERN.matcher(descTemplate);
      StringBuilder sb = new StringBuilder();
      while (matcher.find()) {
        String expr = matcher.group(1);
        Object value = PARSER.parseExpression(expr).getValue(context);
        matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
      }
      matcher.appendTail(sb);
      return sb.toString();
    } catch (Exception e) {
      log.warn(
          "SpEL expression resolution failed, using original description: {}", descTemplate, e);
      return descTemplate;
    }
  }

  /**
   * Builds the operation log entity.
   *
   * @param annotation the annotation instance
   * @param description the resolved description string
   * @param duration the method execution duration (milliseconds)
   * @return the operation log entity
   */
  private SysOperationLog buildLogEntity(
      OperationLog annotation, String description, long duration) {
    SysOperationLog logEntity = new SysOperationLog();
    logEntity.setTraceId(MDC.get(CommonConstants.MDC_TRACE_ID_KEY));
    logEntity.setModule(annotation.module());
    logEntity.setSubModule(annotation.subModule());
    logEntity.setOperationType(annotation.type().name());
    logEntity.setDescription(description);
    logEntity.setDuration(duration);
    logEntity.setUsername(MDC.get(CommonConstants.MDC_CURRENT_USER_KEY));
    logEntity.setClientIp(LogUtil.getClientIp());

    // Set request context from RequestContextHolder
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        logEntity.setRequestMethod(request.getMethod());
        logEntity.setRequestUrl(request.getRequestURI());
      }
    } catch (Exception e) {
      log.warn("Failed to retrieve request context for operation log: {}", e.getMessage());
    }

    logEntity.setCreateTime(LocalDateTime.now());
    return logEntity;
  }
}
