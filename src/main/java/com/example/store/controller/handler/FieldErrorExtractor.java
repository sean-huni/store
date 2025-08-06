package com.example.store.controller.handler;

import com.example.store.dto.error.ViolationDTO;
import org.springframework.context.MessageSource;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

class FieldErrorExtractor {
    private final MessageSource messageSource;
    private final Locale locale;


    FieldErrorExtractor(final MessageSource messageSource, final Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    List<ViolationDTO> extractErrorObjects(final List<FieldError> fieldErrors) {
        return fieldErrors.stream().map(fieldError -> {
            final ViolationDTO violationDTO = new ViolationDTO();
            violationDTO.setField(fieldError.getField());
            if (fieldError.getRejectedValue() != null) {
                violationDTO.setRejectedValue(fieldError.getRejectedValue().toString());
            } else {
                violationDTO.setRejectedValue("");
            }

            // Resolve the message using MessageSource
            final String defaultMessage = fieldError.getDefaultMessage();
            if (defaultMessage != null && defaultMessage.startsWith("global.400")) {
                violationDTO.setErrMsg(messageSource.getMessage(defaultMessage, fieldError.getArguments(), defaultMessage, locale));
            } else {
                violationDTO.setErrMsg(defaultMessage);
            }

            return violationDTO;
        }).collect(Collectors.toList());
    }

    List<ViolationDTO> extractErrorObjects(final HandlerMethodValidationException exception) {
        final List<ViolationDTO> violationDTOS = new ArrayList<>();
        exception.getAllErrors().forEach(err -> {
            ViolationDTO violationDTO = new ViolationDTO();
            if (err instanceof FieldError fieldError) {
                violationDTO.setField(fieldError.getField());
                if (fieldError.getRejectedValue() != null) {
                    violationDTO.setRejectedValue(fieldError.getRejectedValue().toString());
                } else {
                    violationDTO.setRejectedValue("");
                }

                // Resolve the message using MessageSource
                final String defaultMessage = fieldError.getDefaultMessage();
                if (defaultMessage != null && defaultMessage.startsWith("global.400")) {
                    violationDTO.setErrMsg(messageSource.getMessage(defaultMessage, fieldError.getArguments(), defaultMessage, locale));
                } else {
                    violationDTO.setErrMsg(defaultMessage);
                }
            } else {
                violationDTO.setField("global");
                violationDTO.setRejectedValue("null");

                // Resolve the message using MessageSource
                final String defaultMessage = err.getDefaultMessage();
                if (defaultMessage != null && defaultMessage.startsWith("global.400")) {
                    violationDTO.setErrMsg(messageSource.getMessage(defaultMessage, err.getArguments(), defaultMessage, locale));
                } else {
                    violationDTO.setErrMsg(defaultMessage);
                }
            }
            violationDTOS.add(violationDTO);
        });
        return violationDTOS;
    }
}