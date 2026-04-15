package com.lincsoft.validator;

import com.lincsoft.annotation.ValidRoleCode;
import com.lincsoft.constant.RoleCodeEnums;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for {@link ValidRoleCode}.
 *
 * <p>Determine whether the incoming string exists in {@link RoleCodeEnums#getValidCodes()}.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
public class RoleCodeValidator implements ConstraintValidator<ValidRoleCode, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return true;
    }
    return RoleCodeEnums.getValidCodes().contains(value);
  }
}
