package com.lincsoft.constant;

/**
 * Subject type enumeration for row-level data permission grants.
 *
 * <p>Identifies the type of principal that a permission grant is assigned to:
 *
 * <ul>
 *   <li>{@link #USER} – grant applies to a specific user
 *   <li>{@link #ROLE} – grant applies to all users with the specified role
 *   <li>{@link #DEPT} – grant applies to all users belonging to the specified department
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public enum SubjectType {
  USER,
  ROLE,
  DEPT
}
