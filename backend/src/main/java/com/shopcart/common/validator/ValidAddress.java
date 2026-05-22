package com.shopcart.common.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validator for shipping addresses
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidAddressValidator.class)
@Documented
public @interface ValidAddress {
    String message() default "Address must be between 5 and 255 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
