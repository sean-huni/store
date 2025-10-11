package com.example.store.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Bearer token format.
 * Validates that a string follows the "Bearer {token}" format and that the token is not empty.
 */
public class BearerTokenValidator implements ConstraintValidator<BearerToken, String> {

    @Override
    public void initialize(final BearerToken constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        // Null values should be handled by @NotNull if required
        if (value == null) {
            return false;
        }

        // Check if the value is empty
        if (value.trim().isEmpty()) {
            return false;
        }

        // Check if the value starts with "Bearer "
        if (!value.startsWith("Bearer ")) {
            return false;
        }

        // Extract the token part and check if it's not empty
        final String token = value.substring("Bearer ".length());
        return !token.isEmpty();
    }
}