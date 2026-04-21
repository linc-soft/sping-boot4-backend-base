package com.lincsoft.validator;

import com.lincsoft.annotation.ValidEnum;
import com.lincsoft.common.BaseEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validator implementation for {@link ValidEnum}.
 *
 * <p>Extracts all {@code getCode()} values from the specified {@link BaseEnum} enum class and
 * checks whether the input string matches one of them (compared via {@link String#valueOf}).
 *
 * @author 林创科技
 * @since 2026-04-21
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

  /** Cached set of valid code strings for the target enum. */
  private Set<String> validCodes;

  @Override
  public void initialize(ValidEnum annotation) {
    Class<? extends BaseEnum<?>> enumClass = annotation.value();
    BaseEnum<?>[] constants = enumClass.getEnumConstants();
    validCodes =
        Stream.of(constants).map(e -> String.valueOf(e.getCode())).collect(Collectors.toSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return validCodes.contains(value);
  }
}
