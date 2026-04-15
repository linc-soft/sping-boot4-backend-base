package com.lincsoft.interceptor;

/**
 * Thread-local holder for the current authenticated user's numeric ID.
 *
 * <p>Set by {@code JwtAuthorizationFilter} after successful authentication and cleared at the end
 * of each request. Used by {@link DataPermissionInterceptor} to avoid repeated database lookups for
 * the username → user ID mapping within a single request.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public final class DataPermissionUserHolder {

  private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

  private DataPermissionUserHolder() {}

  /**
   * Set the current user ID for the current thread.
   *
   * @param userId user ID
   */
  public static void setCurrentUserId(Long userId) {
    USER_ID_HOLDER.set(userId);
  }

  /**
   * Get the current user ID for the current thread.
   *
   * @return user ID, or null if not set
   */
  public static Long getCurrentUserId() {
    return USER_ID_HOLDER.get();
  }

  /** Clear the current user ID. Must be called at the end of each request to prevent leaks. */
  public static void clear() {
    USER_ID_HOLDER.remove();
  }
}
