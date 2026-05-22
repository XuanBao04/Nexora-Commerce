package com.shopcart.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validator for Vietnamese phone numbers
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number format. Expected Vietnamese phone number (10-11 digits)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
