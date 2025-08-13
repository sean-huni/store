package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Factory for creating error response DTOs with proper localization.
 */
@Component
@RequiredArgsConstructor
public class ErrorRespFactory {

    private final MessageSource messageSource;

    /**
     * Creates an ErrorDTO with the given parameters.
     *
     * @param status         the HTTP status
     * @param messageKey     the message key for localization
     * @param args           the arguments for message interpolation
     * @param defaultMessage the default message if the key cannot be resolved
     * @param violations     the list of validation violations
     * @return the created ErrorDTO
     */
    public ErrorDTO create(final HttpStatus status, final String messageKey,
                           final Object[] args, final String defaultMessage,
                           final List<ViolationDTO> violations) {

        ErrorDTO errorDTO = new ErrorDTO();

        // Set the name to the status name (e.g., "BAD_REQUEST")
        errorDTO.setName(status.name());

        // Resolve the message using the messageKey and args
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageKey != null
                ? messageSource.getMessage(messageKey, args, defaultMessage, locale)
                : defaultMessage;
        errorDTO.setMessage(message);

        // Set the violations
        errorDTO.setViolations(violations);

        // Set the timestamp to the current time
        errorDTO.setTimestamp(ZonedDateTime.now());

        return errorDTO;
    }
}