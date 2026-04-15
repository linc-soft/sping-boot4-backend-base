package com.lincsoft.annotation;

import com.lincsoft.validator.RoleCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Verify whether the field value is a valid RoleCode.
 *
 * <p>Supports {@code null} values (please use with {@code @NotNull} if non-null is required).
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Documented
@Constraint(validatedBy = RoleCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoleCode {

  String message() default "Invalid role code. Must be one of the predefined role codes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
