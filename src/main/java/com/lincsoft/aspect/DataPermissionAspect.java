package com.lincsoft.aspect;

import com.lincsoft.annotation.DataPermission;
import com.lincsoft.constant.ResourceType;
import com.lincsoft.services.master.DataPermissionService;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces data permission checks on methods annotated with {@link DataPermission}.
 *
 * <p>Intercepts the annotated method before execution, extracts the resource ID via SpEL
 * evaluation, and delegates to {@link DataPermissionService#checkPermission} to verify that the
 * current user holds the required permission. If the check fails, a {@code
 * BusinessException(FORBIDDEN)} is thrown and the target method is not executed.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @DataPermission(
 *     resourceType = ResourceType.ORDER,
 *     resourceIdParam = "#id",
 *     permission = PermissionBit.WRITE
 * )
 * public void updateOrder(Long id, OrderUpdateRequest req) { ... }
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DataPermissionAspect {

  private final DataPermissionService dataPermissionService;

  private static final ExpressionParser PARSER = new SpelExpressionParser();

  /**
   * Before advice: intercepts methods annotated with {@link DataPermission} and performs the
   * permission check before the target method executes.
   *
   * @param joinPoint the join point
   * @param dataPermission the annotation instance
   * @throws Throwable if the permission check fails (BusinessException) or SpEL evaluation fails
   */
  @Before("@annotation(dataPermission)")
  public void before(JoinPoint joinPoint, DataPermission dataPermission) {
    // Extract resource ID from method arguments using SpEL
    Long resourceId = resolveResourceId(joinPoint, dataPermission.resourceIdParam());

    ResourceType resourceType = dataPermission.resourceType();

    // Delegate permission check to the service (throws BusinessException(FORBIDDEN) if denied)
    dataPermissionService.checkPermission(resourceType, resourceId, dataPermission.permission());
  }

  /**
   * Evaluate the SpEL expression to extract the resource ID from the method arguments.
   *
   * @param joinPoint the join point providing method signature and arguments
   * @param spelExpression the SpEL expression string (e.g. {@code "#id"}, {@code "#req.orderId"})
   * @return the resolved resource ID as a Long
   * @throws IllegalArgumentException if the expression cannot be evaluated or the result is not a
   *     Long
   */
  private Long resolveResourceId(JoinPoint joinPoint, String spelExpression) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    String[] paramNames = signature.getParameterNames();
    Object[] args = joinPoint.getArgs();

    // Build evaluation context with method parameters as variables
    StandardEvaluationContext context = new StandardEvaluationContext();
    if (paramNames != null) {
      for (int i = 0; i < paramNames.length; i++) {
        context.setVariable(paramNames[i], args[i]);
      }
    }

    try {
      Object value = PARSER.parseExpression(spelExpression).getValue(context);
      if (value instanceof Long id) {
        return id;
      }
      if (value instanceof Number number) {
        return number.longValue();
      }
      throw new IllegalArgumentException(
          "SpEL expression '"
              + spelExpression
              + "' on method '"
              + method.getName()
              + "' did not resolve to a numeric resource ID, got: "
              + (value == null ? "null" : value.getClass().getName()));
    } catch (Exception e) {
      log.error(
          "Failed to resolve resourceId from SpEL expression '{}' on method '{}': {}",
          spelExpression,
          method.getName(),
          e.getMessage());
      throw new IllegalArgumentException(
          "Cannot resolve resource ID from expression: " + spelExpression, e);
    }
  }
}
