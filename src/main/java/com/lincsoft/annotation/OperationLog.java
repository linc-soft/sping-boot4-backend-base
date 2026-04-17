package com.lincsoft.annotation;

import com.lincsoft.constant.OperationType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that should be recorded in the operation log.
 *
 * <p>Apply this annotation to controller or service methods to automatically capture operation
 * details such as module, submodule, operation type, and description for audit purposes.
 *
 * @author 林创科技
 * @since 2026-04-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
  /**
   * Gets the module name.
   *
   * @return the module name, empty string if not specified
   */
  String module() default "";

  /**
   * Gets the submodule name.
   *
   * @return the submodule name, empty string if not specified
   */
  String subModule() default "";

  /**
   * Gets the operation type.
   *
   * @return the type of operation being performed, default is OTHER
   */
  OperationType type() default OperationType.OTHER;

  /**
   * Gets the operation description.
   *
   * <p>Supports SpEL expressions using {@code #{#variableName}} syntax for dynamic content.
   * Expressions are evaluated using a {@code StandardEvaluationContext} that supports both property
   * access and method invocation.
   *
   * <p>Supported expression patterns:
   *
   * <ul>
   *   <li>{@code #{#paramName}} — method parameter value
   *   <li>{@code #{#paramName.property}} — nested property of a method parameter
   *   <li>{@code #{#paramName.method()}} — method invocation on a method parameter
   *   <li>{@code #{#result}} — method return value (null if method threw an exception)
   *   <li>{@code #{#result.property}} — nested property of the return value
   *   <li>{@code #{#result.method()}} — method invocation on the return value
   * </ul>
   *
   * <p>Example: {@code @OperationLog(description = "Created user #{#dto.username}")}
   *
   * @return a human-readable description of the operation, default is empty string
   */
  String description() default "";
}
