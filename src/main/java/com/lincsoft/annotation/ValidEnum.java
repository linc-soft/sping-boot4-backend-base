package com.lincsoft.annotation;

import com.lincsoft.common.BaseEnum;
import com.lincsoft.validator.EnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validate whether the field value matches one of the codes defined in the specified {@link
 * BaseEnum} implementation.
 *
 * <p>Supports {@code null} values (use with {@code @NotNull} or {@code @NotBlank} if non-null is
 * required).
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @ValidEnum(RoleCodeEnums.class)
 * private String roleCode;
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

  /** The {@link BaseEnum} implementation class to validate against. */
  Class<? extends BaseEnum<?>> value();

  String message() default "Invalid value. Must be one of the predefined enum codes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
