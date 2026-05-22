package com.nexoracommerce.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for addresses
 */
public class ValidAddressValidator implements ConstraintValidator<ValidAddress, String> {

    private static final int MIN_LENGTH = 5;
    private static final int MAX_LENGTH = 255;

    @Override
    public void initialize(ValidAddress annotation) {
        // Initialization if needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Let @NotBlank handle null/empty validation
        }
        return value.length() >= MIN_LENGTH && value.length() <= MAX_LENGTH;
    }
}
