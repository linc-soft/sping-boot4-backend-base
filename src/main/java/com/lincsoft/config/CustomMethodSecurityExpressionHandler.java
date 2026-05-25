package com.lincsoft.config;

import com.lincsoft.constant.RoleCodeEnums;
import java.util.List;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Custom method security expression handler that grants ADMIN users all role authorities.
 *
 * <p>In Spring Security 7.x, the actual entry point for expression evaluation is {@code
 * createEvaluationContext(Supplier, MethodInvocation)}, not the overridden {@code
 * createSecurityExpressionRoot(Authentication, MethodInvocation)} from earlier versions. This
 * implementation overrides the new entry point to replace the authentication object with one
 * containing all valid role authorities when the current user has the {@code ROLE_ADMIN} authority.
 *
 * <p>This ensures that any {@code @PreAuthorize("hasRole(...)")} or {@code hasAnyRole(...)} check
 * will always pass for ADMIN users, without modifying existing annotations.
 *
 * @author 林创科技
 * @since 2026-05-25
 */
public class CustomMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

  /** All valid role authorities derived from {@link RoleCodeEnums}, with ROLE_ prefix applied. */
  private static final List<SimpleGrantedAuthority> ALL_AUTHORITIES =
      RoleCodeEnums.getValidCodes().stream()
          .map(code -> new SimpleGrantedAuthority(code.startsWith("ROLE_") ? code : "ROLE_" + code))
          .toList();

  /**
   * Creates the evaluation context for SpEL expression evaluation.
   *
   * <p>If the current user has the ADMIN role, replaces the authentication with one that holds all
   * valid role authorities so that any {@code hasRole()} / {@code hasAnyRole()} check returns true.
   * Otherwise, delegates to the default implementation.
   *
   * @param authentication the supplier for the current authentication object
   * @param invocation the method invocation being secured
   * @return the evaluation context with the appropriate root object
   */
  @Override
  public EvaluationContext createEvaluationContext(
      Supplier<? extends @Nullable Authentication> authentication, MethodInvocation invocation) {
    Authentication auth = authentication.get();
    if (isAdmin(auth)) {
      Authentication adminAuth =
          new UsernamePasswordAuthenticationToken(
              auth.getPrincipal(), auth.getCredentials(), ALL_AUTHORITIES);
      return super.createEvaluationContext(() -> adminAuth, invocation);
    }
    return super.createEvaluationContext(authentication, invocation);
  }

  /**
   * Checks whether the given authentication belongs to an ADMIN user.
   *
   * @param auth the authentication to check (may be null)
   * @return true if the user has the ROLE_ADMIN authority
   */
  private boolean isAdmin(@Nullable Authentication auth) {
    if (auth == null) {
      return false;
    }
    return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }
}
