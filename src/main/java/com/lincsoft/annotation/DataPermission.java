package com.lincsoft.annotation;

import com.lincsoft.constant.PermissionBit;
import com.lincsoft.constant.ResourceType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a method requires data permission verification before execution.
 *
 * <p>The AOP aspect {@code DataPermissionAspect} intercepts all methods annotated with this
 * annotation and calls {@code DataPermissionService.checkPermission} to verify that the current
 * user holds the required permission on the target resource. If the check fails, a {@code
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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {

  /**
   * The type of resource being protected.
   *
   * @return resource type enum value
   */
  ResourceType resourceType();

  /**
   * SpEL expression used to extract the resource ID from the method arguments.
   *
   * <p>Examples: {@code "#id"}, {@code "#req.orderId"}, {@code "#entity.id"}
   *
   * @return SpEL expression string
   */
  String resourceIdParam();

  /**
   * The permission bit required to execute the annotated method.
   *
   * @return required permission bit
   */
  PermissionBit permission();
}
