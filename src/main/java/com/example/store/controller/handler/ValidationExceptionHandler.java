package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Log4j2
@RestControllerAdvice
public class ValidationExceptionHandler {
    private static final String GLOBAL_ERROR_CODE = "global.400.000";
    private static final Locale locale = Locale.getDefault();
    private final MessageSource messageSource;
    private static final Pattern QUOTED_TEXT_PATTERN = Pattern.compile("\"([^\"]*)\"");

    public ValidationExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

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

        // Extract violationDTOS from the validation exception using FieldErrorExtractor with MessageSource
        final List<ViolationDTO> violationDTOS = new FieldErrorExtractor(messageSource, locale).extractErrorObjects(invalidException);

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

        // Extract violationDTOS from the validation exception using FieldErrorExtractor with MessageSource
        final List<ViolationDTO> violationDTOS = new FieldErrorExtractor(messageSource, locale)
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

        List<ViolationDTO> violations = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            ViolationDTO violationDTO = new ViolationDTO();
            violationDTO.setField(violation.getPropertyPath().toString());
            violationDTO.setRejectedValue(violation.getInvalidValue() != null ?
                    violation.getInvalidValue().toString() : "");

            // Resolve the message using MessageSource if it's a global message code
            String message = violation.getMessage();
            if (message != null && message.startsWith("global.400")) {
                violationDTO.setErrMsg(messageSource.getMessage(message, null, message, locale));
            } else {
                violationDTO.setErrMsg(message);
            }

            violations.add(violationDTO);
        });

        return new ErrorDTO(HttpStatus.BAD_REQUEST.name(), "Validation failed", violations, ZonedDateTime.now());
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
        final String errorMessage = messageSource.getMessage(ex.getMessage(), null, ex.getMessage(), locale);

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
        final String errorMessage = messageSource.getMessage(GLOBAL_ERROR_CODE, null, GLOBAL_ERROR_CODE, locale);

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
        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                ex.getMessage(),
                null,
                ZonedDateTime.now()
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleBadCredentialsException(final BadCredentialsException ex) {
        log.error("Bad credentials", ex);
        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                "Invalid email or password",
                null,
                ZonedDateTime.now()
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDTO handleUsernameNotFoundException(final UsernameNotFoundException ex) {
        log.error("Username not found", ex);
        return new ErrorDTO(
                HttpStatus.UNAUTHORIZED.name(),
                "User not found",
                null,
                ZonedDateTime.now()
        );
    }
}