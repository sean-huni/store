package com.example.store.controller.handler;

import com.example.store.dto.error.ViolationDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.nonNull;

/**
 * Component responsible for extracting and converting validation errors
 * into ViolationDTO objects with properly resolved i18n messages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FieldErrorExtractor {
    private final MessageSource messageSource;

    // Constants for better maintainability
    private static final String GLOBAL_FIELD = "global";
    private static final String NULL_VALUE = "null";
    private static final String EMPTY_STRING = "";
    private static final int MAX_VALUE_LENGTH = 100;

    /**
     * Extracts validation errors from a list of FieldError objects.
     *
     * @param fieldErrors the list of field errors to process
     * @return a list of ViolationDTO objects representing the errors
     */
    public List<ViolationDTO> extractErrorObjects(final List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(this::convertFieldErrorToViolationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Extracts validation errors from a HandlerMethodValidationException.
     *
     * @param exception the exception containing validation errors
     * @return a list of ViolationDTO objects representing the errors
     */
    public List<ViolationDTO> extractErrorObjects(final HandlerMethodValidationException exception) {
        final List<ViolationDTO> violationDTOS = new ArrayList<>();
        exception.getAllErrors().forEach(err -> {
            if (err instanceof FieldError fieldError) {
                violationDTOS.add(convertFieldErrorToViolationDTO(fieldError));
            } else {
                violationDTOS.add(convertMessageSourceResolvableToViolationDTO(err));
            }
        });
        return violationDTOS;
    }

    /**
     * Extracts validation errors from a ConstraintViolationException.
     *
     * @param exception the exception containing constraint violations
     * @return a list of ViolationDTO objects representing the violations
     */
    public List<ViolationDTO> extractErrorObjects(final ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
                .map(this::convertConstraintViolationToViolationDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converts a FieldError to a ViolationDTO.
     *
     * @param fieldError the field error to convert
     * @return a ViolationDTO representing the field error
     */
    private ViolationDTO convertFieldErrorToViolationDTO(final FieldError fieldError) {
        final ViolationDTO violationDTO = new ViolationDTO();
        violationDTO.setField(fieldError.getField());
        violationDTO.setRjctValue(formatRejectedValue(fieldError.getRejectedValue()));

        // Resolve the message using MessageSource with proper locale
        final String defaultMessage = fieldError.getDefaultMessage();
        final String resolvedMessage = resolveErrorMessage(defaultMessage, fieldError.getArguments());
        violationDTO.setErrMsg(resolvedMessage);

        // Optionally set error code if the message was a key
        if (isMessageKey(defaultMessage)) {
            violationDTO.setErrCode(defaultMessage);
        }

        return violationDTO;
    }

    /**
     * Converts a MessageSourceResolvable to a ViolationDTO.
     *
     * @param resolvable the message source resolvable to convert
     * @return a ViolationDTO representing the message source resolvable
     */
    private ViolationDTO convertMessageSourceResolvableToViolationDTO(final MessageSourceResolvable resolvable) {
        final ViolationDTO violationDTO = new ViolationDTO();
        violationDTO.setField(GLOBAL_FIELD);
        violationDTO.setRjctValue(NULL_VALUE);

        // Resolve the message using MessageSource with proper locale
        final String defaultMessage = resolvable.getDefaultMessage();
        final String resolvedMessage = resolveErrorMessage(defaultMessage, resolvable.getArguments());
        violationDTO.setErrMsg(resolvedMessage);

        // Optionally set error code if the message was a key
        if (isMessageKey(defaultMessage)) {
            violationDTO.setErrCode(defaultMessage);
        }

        return violationDTO;
    }

    /**
     * Converts a ConstraintViolation to a ViolationDTO.
     *
     * @param violation the constraint violation to convert
     * @return a ViolationDTO representing the constraint violation
     */
    private ViolationDTO convertConstraintViolationToViolationDTO(final ConstraintViolation<?> violation) {
        final ViolationDTO violationDTO = new ViolationDTO();

        // Extract proper field name from property path
        violationDTO.setField(extractFieldName(violation.getPropertyPath()));
        violationDTO.setRjctValue(formatRejectedValue(violation.getInvalidValue()));

        // Resolve the message using MessageSource with proper locale
        final String message = violation.getMessage();
        final String resolvedMessage = resolveErrorMessage(message, extractConstraintArguments(violation));
        violationDTO.setErrMsg(resolvedMessage);

        // Optionally set error code if the message was a key
        if (isMessageKey(message)) {
            violationDTO.setErrCode(message);
        }

        return violationDTO;
    }

    /**
     * Resolves an error message using the MessageSource with the current request's locale.
     *
     * @param message   the message key or literal message to resolve
     * @param arguments the message arguments for interpolation
     * @return the resolved message
     */
    private String resolveErrorMessage(final String message, final Object[] arguments) {
        if (!nonNull(message)) {
            return "Validation failed"; // Default fallback message
        }

        // Get the locale from the current request context
        final Locale locale = LocaleContextHolder.getLocale();

        // Check if it looks like a message key
        if (isMessageKey(message)) {
            try {
                // Try to resolve as a message key
                String resolved = messageSource.getMessage(message, arguments, null, locale);
                if (resolved != null) {
                    return resolved;
                }
            } catch (Exception e) {
                log.debug("Could not resolve message key '{}', using as default message", message);
            }
        }

        // If not a key or resolution failed, try to handle placeholder syntax {message.key}
        if (message.startsWith("{") && message.endsWith("}")) {
            String keyWithoutBraces = message.substring(1, message.length() - 1);
            try {
                String resolved = messageSource.getMessage(keyWithoutBraces, arguments, null, locale);
                if (resolved != null) {
                    return resolved;
                }
            } catch (Exception e) {
                log.debug("Could not resolve placeholder message '{}', using fallback", keyWithoutBraces);
            }
        }

        // Return the message as-is if it's not a key or couldn't be resolved
        return message;
    }

    /**
     * Formats the rejected value for display, truncating if too long.
     *
     * @param value the rejected value
     * @return formatted string representation of the value
     */
    private String formatRejectedValue(final Object value) {
        if (value == null) {
            return NULL_VALUE;
        }

        final String stringValue = value.toString();

        // Truncate very long values for readability
        if (stringValue.length() > MAX_VALUE_LENGTH) {
            return stringValue.substring(0, MAX_VALUE_LENGTH - 3) + "...";
        }

        return stringValue.isEmpty() ? EMPTY_STRING : stringValue;
    }

    /**
     * Extracts the field name from a property path.
     *
     * @param propertyPath the property path from constraint violation
     * @return the extracted field name
     */
    private String extractFieldName(final Path propertyPath) {
        if (propertyPath == null) {
            return "unknown";
        }

        try {
            return StreamSupport.stream(propertyPath.spliterator(), false)
                    .reduce((first, second) -> second)
                    .map(Path.Node::getName)
                    .orElse("unknown");
        } catch (Exception e) {
            log.debug("Failed to extract field name from property path, using toString() fallback", e);
            // Fallback to toString() if spliterator fails
            String pathString = propertyPath.toString();
            if (pathString != null && !pathString.isEmpty()) {
                // Extract the last part after the last dot
                int lastDotIndex = pathString.lastIndexOf('.');
                return lastDotIndex >= 0 ? pathString.substring(lastDotIndex + 1) : pathString;
            }
            return "unknown";
        }
    }

    /**
     * Checks if a string appears to be a message key rather than a literal message.
     * Message keys typically contain dots and no spaces, following patterns like "order.400.001"
     *
     * @param value the string to check
     * @return true if it appears to be a message key, false otherwise
     */
    private boolean isMessageKey(final String value) {
        return value != null &&
                value.contains(".") &&
                !value.contains(" ") &&
                !value.contains("\n");
    }

    /**
     * Extracts arguments from a constraint violation for message interpolation.
     * Uses a generic approach to extract common constraint attributes without
     * constraint-specific logic, making it extensible and maintainable.
     *
     * @param violation the constraint violation
     * @return array of arguments for message interpolation
     */
    private Object[] extractConstraintArguments(final ConstraintViolation<?> violation) {
        if (violation == null) {
            return null;
        }

        try {
            // Check if constraint descriptor is available
            var constraintDescriptor = violation.getConstraintDescriptor();
            if (constraintDescriptor == null) {
                return null;
            }

            // Get constraint attributes map
            final var attributes = constraintDescriptor.getAttributes();
            if (attributes == null) {
                return null;
            }

            // Common constraint attributes to extract in order of priority
            final String[] commonAttributes = {"max", "min", "value", "regexp", "fraction", "integer"};

            // Try to extract the most relevant attribute
            for (String attributeName : commonAttributes) {
                Object attributeValue = attributes.get(attributeName);
                if (attributeValue != null && !isDefaultAttributeValue(attributeName, attributeValue)) {
                    return new Object[]{attributeValue};
                }
            }

            // For constraints that might benefit from the invalid value in the message
            if (shouldIncludeInvalidValue(violation)) {
                Object invalidValue = violation.getInvalidValue();
                if (invalidValue != null) {
                    return new Object[]{invalidValue};
                }
            }

        } catch (Exception e) {
            log.debug("Failed to extract constraint attributes from violation", e);
        }

        // Return null to let MessageSource handle default interpolation
        return null;
    }

    /**
     * Checks if the attribute value is a default value that shouldn't be used for interpolation.
     */
    private boolean isDefaultAttributeValue(String attributeName, Object value) {
        // Common default values that indicate the attribute wasn't explicitly set
        return switch (attributeName) {
            case "min" -> Integer.valueOf(0).equals(value);
            case "max" -> Integer.valueOf(Integer.MAX_VALUE).equals(value);
            default -> false;
        };
    }

    /**
     * Determines if the invalid value should be included in the error message.
     */
    private boolean shouldIncludeInvalidValue(ConstraintViolation<?> violation) {
        try {
            var constraintDescriptor = violation.getConstraintDescriptor();
            if (constraintDescriptor == null) {
                return false;
            }

            var annotation = constraintDescriptor.getAnnotation();
            if (annotation == null) {
                return false;
            }

            String annotationType = annotation.annotationType().getSimpleName();
            // Include invalid value for constraints where it provides useful context
            return "Email".equals(annotationType) || "Pattern".equals(annotationType);
        } catch (Exception e) {
            log.debug("Failed to determine if invalid value should be included", e);
            return false;
        }
    }
}