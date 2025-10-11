package com.example.store.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for Bearer token format.
 * Validates that a string follows the "Bearer {token}" format and that the token is not empty.
 */
@Documented
@Constraint(validatedBy = BearerTokenValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BearerToken {

    String message() default "auth.401.002";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}