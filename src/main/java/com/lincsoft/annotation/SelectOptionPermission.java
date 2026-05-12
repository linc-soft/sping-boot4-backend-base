package com.lincsoft.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying required permission on {@link com.lincsoft.common.SelectOptionProvider}
 * implementations.
 *
 * <p>When a SelectOptionProvider is annotated with this annotation, the SelectOptionController will
 * check if the current user has the required permission before returning the select options.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Component
 * @SelectOptionPermission("USER_VIEW")
 * public class RoleSelectOptionProvider implements SelectOptionProvider {
 *     // ...
 * }
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-05-12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SelectOptionPermission {
  /**
   * The required permission code for accessing this select option.
   *
   * <p>Empty string means no permission check is required (public access).
   *
   * <p>Permission codes should match the role codes defined in {@link
   * com.lincsoft.constant.RoleCodeEnums}, such as "USER_VIEW", "ROLE_VIEW", etc.
   *
   * @return the permission code, empty string for no permission check
   */
  String value() default "";
}
