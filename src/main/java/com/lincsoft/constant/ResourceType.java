package com.lincsoft.constant;

/**
 * Resource type enumeration for data permission control.
 *
 * <p>Each value identifies a business entity type that is subject to row-level data permission
 * filtering. Business modules should add new resource types here when introducing new entities that
 * require data permission control.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * // Add a new resource type for the order module:
 * ORDER,
 * CUSTOMER,
 * CONTRACT
 * }</pre>
 *
 * <p>The string value returned by {@link #value()} is stored in the {@code resource_type} column of
 * {@code mst_data_permission_grant}.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public enum ResourceType {
// Add business resource types here, e.g.: ORDER, CUSTOMER, CONTRACT
;

  /**
   * Returns the string value stored in {@code mst_data_permission_grant.resource_type}.
   *
   * @return the enum name as a string
   */
  public String value() {
    return this.name();
  }
}
