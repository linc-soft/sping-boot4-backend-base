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
   * @return a human-readable description of the operation, default is empty string
   */
  String description() default "";
}
