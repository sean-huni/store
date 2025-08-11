package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.exception.LocalizedJsonParseException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ValidationExceptionHandler {
    private static final String GLOBAL_ERROR_CODE = "global.400.000";
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("\"([^\"]*)\"");

    private final MessageSource messageSource;
    private final FieldErrorExtractor fieldErrorExtractor;

    /**
     * Extracts text inside double quotes from a message.
     * If no quoted text is found, returns the original message.
     *
     * @param message the message to extract quoted text from
     * @return the text inside double quotes, or the original message if no quotes found
     */
    private String extractQuotedText(final String message) {
        if (message == null) {
            return null;
        }

        final Matcher matcher = QUOTED_TEXT_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return message;
    }

    /**
     * Handles HandlerMethodValidationException which occurs when method parameter validation fails.
     *
     * @param invalidException the exception to handle
     * @return a ResponseEntity containing error details
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorDTO invalidInputHandler(final HandlerMethodValidationException invalidException) {
        // Extract the default message from the exception
        final String rawErrorMessage = invalidException.getMessage();

        // Extract only the text inside double quotes
        final String errorMessage = extractQuotedText(rawErrorMessage);
        log.error(errorMessage, invalidException);

        // Extract violationDTOS from the validation exception using FieldErrorExtractor
        final List<ViolationDTO> violationDTOS = fieldErrorExtractor.extractErrorObjects(invalidException);

        return new ErrorDTO(null, errorMessage, violationDTOS, ZonedDateTime.now());
    }

    /**
     * Handles MethodArgumentNotValidException which occurs when @Valid validation fails.
     *
     * @param invalidException the exception to handle
     * @return a ResponseEntity containing error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorDTO invalidInputHandler(final MethodArgumentNotValidException invalidException) {
        final String rawErrorMessage = invalidException.getMessage();
        final String errorMessage = extractQuotedText(rawErrorMessage);

        // Extract violationDTOS from the validation exception using FieldErrorExtractor
        final List<ViolationDTO> violationDTOS = fieldErrorExtractor
                .extractErrorObjects(invalidException.getBindingResult().getFieldErrors());
        return new ErrorDTO(null, errorMessage, violationDTOS, ZonedDateTime.now());
    }

    /**
     * Handles ConstraintViolationException which occurs when method parameter validation fails.
     *
     * @param ex the exception to handle
     * @return a ResponseEntity containing error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    ErrorDTO handleConstraintViolationException(final ConstraintViolationException ex) {
        log.error("Validation error", ex);

        // Extract violationDTOS from the constraint violation exception using FieldErrorExtractor
        final List<ViolationDTO> violations = fieldErrorExtractor.extractErrorObjects(ex);

        // Extract the message from the message source
        final String errorMessage = messageSource.getMessage("global.400.001", null, "Validation failed", Locale.getDefault());

        return new ErrorDTO(HttpStatus.BAD_REQUEST.name(), errorMessage, violations, ZonedDateTime.now());
    }

    /**
     * Handles CustomerNotFoundException which occurs when a specific HTTP status needs to be returned.
     *
     * @param ex the exception to handle
     * @return a ResponseEntity containing error details
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseBody
    ErrorDTO handleCustomerNotFoundException(final CustomerNotFoundException ex) {
        log.error("CustomerNotFoundException", ex);

        // Extract the message from the message source using the GLOBAL_ERROR_CODE
        final String errorMessage = messageSource.getMessage(ex.getMessage(), null, ex.getMessage(), Locale.getDefault());

        final ErrorDTO errorDTO = new ErrorDTO(HttpStatus.NOT_FOUND.name(), errorMessage, null, ZonedDateTime.now());

        return errorDTO;
    }

    /**
     * Handles general exceptions that aren't caught by more specific handlers.
     *
     * @param ex the exception to handle
     * @return a {@link ErrorDTO} containing error details
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    ErrorDTO handleGeneralException(final Exception ex) {
        log.error("Unexpected error occurred", ex);

        // Extract the message from the message source using the GLOBAL_ERROR_CODE
        final String errorMessage = messageSource.getMessage(GLOBAL_ERROR_CODE, null, GLOBAL_ERROR_CODE, Locale.getDefault());

        return new ErrorDTO(HttpStatus.INTERNAL_SERVER_ERROR.name(), errorMessage, null, ZonedDateTime.now());
    }


    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorDTO handleEmailAlreadyExistsException(final EmailAlreadyExistsException ex) {
        log.error("Email already exists", ex);
        return new ErrorDTO(
                HttpStatus.CONFLICT.name(),
                ex.getMessage(),
                null,
                ZonedDateTime.now()
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleInvalidRefreshTokenException(final InvalidRefreshTokenException ex) {
        log.error("Invalid refresh token", ex);

        // Extract the message from the message source using the error code
        final String errorMessage = messageSource.getMessage(ex.getMessage(), null, ex.getMessage(), Locale.getDefault());

        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                errorMessage,
                null,
                ZonedDateTime.now()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleBadCredentialsException(final BadCredentialsException ex) {
        log.error("Bad credentials", ex);

        // Extract the message from the message source
        final String errorMessage = messageSource.getMessage("auth.400.008", null, "Invalid email or password", Locale.getDefault());
        
        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                errorMessage,
                null,
                ZonedDateTime.now()
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleUsernameNotFoundException(final UsernameNotFoundException ex) {
        log.error("Username not found", ex);

        // Extract the message from the message source
        final String errorMessage = messageSource.getMessage("auth.400.009", null, "User not found", Locale.getDefault());
        
        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                errorMessage,
                null,
                ZonedDateTime.now()
        );
    }

    /**
     * Handles MethodArgumentTypeMismatchException which occurs when a method argument cannot be converted to the expected type.
     * This is particularly useful for enum parameters like SortEnumDTO where invalid values should return a 400 error.
     *
     * @param ex the exception to handle
     * @return a {@link ErrorDTO} containing error details
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException ex) {
        log.error("Method argument type mismatch", ex);

        String errorMessage;

        // Check if this is a sortDir parameter error
        if ("sortDir".equals(ex.getName()) && ex.getRequiredType() != null &&
                ex.getRequiredType().isEnum()) {
            // Use the specific error message for sortDir
            errorMessage = messageSource.getMessage("global.400.009", null, "Invalid sort direction", Locale.getDefault());
        } else {
            // Generic error for other type mismatches
            errorMessage = messageSource.getMessage("global.400.010", new Object[]{ex.getName(), ex.getValue()}, "Parameter '" + ex.getName() + "' has invalid value: '" + ex.getValue() + "'", Locale.getDefault());
        }

        return new ErrorDTO(
                HttpStatus.BAD_REQUEST.name(),
                errorMessage,
                null,
                ZonedDateTime.now()
        );
    }

    /**
     * Handles LocalizedJsonParseException which occurs when parsing JSON fails.
     * This exception contains a message key and arguments for localization.
     *
     * @param ex the exception to handle
     * @return a {@link ErrorDTO} containing error details
     */
    @ExceptionHandler(LocalizedJsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleLocalizedJsonParseException(final LocalizedJsonParseException ex) {
        log.error("JSON parse error: {}", ex.getMessage(), ex);

        // Extract the message from the message source using the message key and arguments from the exception
        final String errorMessage = messageSource.getMessage(
                ex.getMessageKey(),
                ex.getArgs(),
                ex.getMessage(), // Use the exception's message as fallback
                Locale.getDefault()
        );

        return new ErrorDTO(
                HttpStatus.BAD_REQUEST.name(),
                errorMessage,
                null,
                ZonedDateTime.now()
        );
    }
}