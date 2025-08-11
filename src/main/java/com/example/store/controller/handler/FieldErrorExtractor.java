package com.example.store.controller.handler;

import com.example.store.dto.error.ViolationDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.example.store.constant.AppConstant.GLOBAL_ERROR_MSG_PREFIX;

@Component
@RequiredArgsConstructor
public class FieldErrorExtractor {
    private final MessageSource messageSource;

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
        violationDTO.setRejectedValue(fieldError.getRejectedValue() != null
                ? fieldError.getRejectedValue().toString()
                : "");

        // Resolve the message using MessageSource
        final String defaultMessage = fieldError.getDefaultMessage();
        violationDTO.setErrMsg(resolveErrorMessage(defaultMessage, fieldError.getArguments()));

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
        violationDTO.setField("global");
        violationDTO.setRejectedValue("null");

        // Resolve the message using MessageSource
        final String defaultMessage = resolvable.getDefaultMessage();
        violationDTO.setErrMsg(resolveErrorMessage(defaultMessage, resolvable.getArguments()));

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
        violationDTO.setField(violation.getPropertyPath().toString());
        violationDTO.setRejectedValue(violation.getInvalidValue() != null
                ? violation.getInvalidValue().toString()
                : "");

        // Resolve the message using MessageSource
        final String message = violation.getMessage();
        violationDTO.setErrMsg(resolveErrorMessage(message, null));

        return violationDTO;
    }

    /**
     * Resolves an error message using the MessageSource if it starts with the global error prefix.
     *
     * @param message   the message to resolve
     * @param arguments the message arguments
     * @return the resolved message
     */
    private String resolveErrorMessage(final String message, final Object[] arguments) {
        if (message != null && message.startsWith(GLOBAL_ERROR_MSG_PREFIX)) {
            return messageSource.getMessage(message, arguments, message, Locale.getDefault());
        }
        return message;
    }
}