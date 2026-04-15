package com.lincsoft.constant;

/**
 * Data scope type enumeration for role-based data permission control.
 *
 * <p>Determines which department data a role can access:
 *
 * <ul>
 *   <li>{@link #ALL} – no department restriction; the user can access all data
 *   <li>{@link #DEPT} – restricted to the configured department only
 *   <li>{@link #DEPT_AND_CHILD} – restricted to the configured department and all its descendants
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public enum DataScopeType {
  /** All data, no department restriction. */
  ALL,

  /** Current department only. */
  DEPT,

  /** Current department and all descendant departments. */
  DEPT_AND_CHILD
}
