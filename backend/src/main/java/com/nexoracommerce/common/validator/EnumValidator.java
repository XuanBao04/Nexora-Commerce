package com.nexoracommerce.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 * Validator implementation for enum values
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Set<String> enumValues;

    @Override
    public void initialize(ValidEnum annotation) {
        enumValues = new HashSet<>();
        Class<? extends Enum<?>> enumClass = annotation.enumClass();
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            enumValues.add(enumConstant.name());
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle null/empty validation
        }
        return enumValues.contains(value.toUpperCase());
    }
}
