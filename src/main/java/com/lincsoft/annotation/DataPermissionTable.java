package com.lincsoft.annotation;

import com.lincsoft.constant.ResourceType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity class as subject to automatic data permission filtering.
 *
 * <p>The {@code DataPermissionInterceptor} checks for this annotation on the entity class
 * corresponding to the target table of each SELECT statement. If the annotation is present, the
 * interceptor injects the appropriate WHERE conditions based on the current user's data
 * permissions. If the annotation is absent, the SQL is passed through unchanged.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @DataPermissionTable(resourceType = ResourceType.ORDER, deptField = "dept_id")
 * @TableName("biz_order")
 * public class BizOrder extends BaseEntity {
 *     private Long deptId;
 *     // ...
 * }
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermissionTable {

  /**
   * The resource type that this entity corresponds to in the data permission system.
   *
   * @return resource type enum value
   */
  ResourceType resourceType();

  /**
   * The column name used for department-based filtering.
   *
   * <p>Defaults to {@code "dept_id"}, which is the standard column name for the department foreign
   * key in business tables.
   *
   * @return column name for dept-based filtering
   */
  String deptField() default "dept_id";
}
