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
import org.springframework.context.i18n.LocaleContextHolder;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
     * Base exception handler that creates ErrorDTO with proper localization
     */
    private ErrorDTO createErrorResponse(final HttpStatus status, final String messageKey,
                                         final Object[] args, final String defaultMessage,
                                         final List<ViolationDTO> violations) {
        final ErrorDTO errorDTO = new ErrorDTO();

        // Set the name to the status name (e.g., "BAD_REQUEST")
        errorDTO.setName(status.name());

        // Resolve the message using the messageKey and args
        final Locale locale = LocaleContextHolder.getLocale();
        final String message = messageKey != null
                ? messageSource.getMessage(messageKey, args, defaultMessage, locale)
                : defaultMessage;

        errorDTO.setMessage(message);

        // Set the violations
        errorDTO.setViolations(violations);

        // Set the timestamp to the current time
        errorDTO.setTimestamp(ZonedDateTime.now());

        return errorDTO;
    }

    /**
     * Handles validation exceptions from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleMethodArgumentNotValid(final MethodArgumentNotValidException ex) {
        log.debug("Validation failed for request", ex);

        List<ViolationDTO> violations = fieldErrorExtractor
                .extractErrorObjects(ex.getBindingResult().getFieldErrors());

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "global.400.001",
                null,
                "Validation failed",
                violations
        );
    }

    /**
     * Handles validation exceptions from method parameters
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleHandlerMethodValidation(final HandlerMethodValidationException ex) {
        log.debug("Method parameter validation failed", ex);

        List<ViolationDTO> violations = fieldErrorExtractor.extractErrorObjects(ex);

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "global.400.001",
                null,
                "Validation failed",
                violations
        );
    }

    /**
     * Handles constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleConstraintViolation(final ConstraintViolationException ex) {
        log.debug("Constraint validation failed", ex);

        List<ViolationDTO> violations = fieldErrorExtractor.extractErrorObjects(ex);

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "global.400.001",
                null,
                "Validation failed",
                violations
        );
    }

    /**
     * Handles business exceptions - CustomerNotFoundException
     */
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDTO handleCustomerNotFound(final CustomerNotFoundException ex) {
        log.debug("Customer not found: {}", ex.getMessage());

        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),    // Assuming exception has message key
                ex.getArgs(),       // Assuming exception has args for i18n
                ex.getMessage(),
                null
        );
    }

    /**
     * Handles email already exists exception
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorDTO handleEmailAlreadyExists(final EmailAlreadyExistsException ex) {
        log.debug("Email already exists: {}", Arrays.stream(ex.getArgs()).findFirst().orElseThrow());

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "auth.409.001",
                ex.getArgs(),
                "Email already exists",
                null
        );
    }

    /**
     * Handles authentication exceptions
     */
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleAuthenticationException(final Exception ex) {
        log.debug("Authentication failed", ex);

        // Use same message for both to avoid user enumeration
        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "auth.401.001",
                null,
                "Invalid credentials",
                null
        );
    }

    /**
     * Handles invalid refresh token
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleInvalidRefreshToken(final InvalidRefreshTokenException ex) {
        log.debug("Invalid refresh token", ex);

        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "auth.401.002",
                null,
                "Invalid or expired refresh token",
                null
        );
    }

    /**
     * Handles type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.debug("Type mismatch for parameter: {}", ex.getName());

        String messageKey = "global.400.010";
        Object[] args = new Object[]{ex.getName(), ex.getValue()};

        // Special handling for known enum parameters
        if ("sortDir".equals(ex.getName()) && ex.getRequiredType() != null
                && ex.getRequiredType().isEnum()) {
            messageKey = "global.400.009";
            args = null;
        }

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                messageKey,
                args,
                String.format("Invalid value for parameter '%s'", ex.getName()),
                null
        );
    }

    /**
     * Handles JSON parse exceptions with localization
     */
    @ExceptionHandler(LocalizedJsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handleJsonParseException(final LocalizedJsonParseException ex) {
        log.debug("JSON parse error", ex);

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessageKey(),
                ex.getArgs(),
                ex.getMessage(),
                null
        );
    }

    /**
     * Handles all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorDTO handleGeneralException(final Exception ex) {
        // Log as error since this is unexpected
        log.error("Unexpected error occurred", ex);

        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GLOBAL_ERROR_CODE,
                null,
                "An unexpected error occurred",
                null
        );
    }
}
