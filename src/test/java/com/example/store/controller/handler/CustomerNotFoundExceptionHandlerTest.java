package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import com.example.store.exception.CustomerNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("CustomerNotFoundExceptionHandler - {Unit}")
@ExtendWith(MockitoExtension.class)
class CustomerNotFoundExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private FieldErrorExtractor fieldErrorExtractor;

    private ValidationExceptionHandler validationExceptionHandler;

    @BeforeEach
    void setUp() {
        validationExceptionHandler = new ValidationExceptionHandler(messageSource, fieldErrorExtractor);
    }

    @Test
    @DisplayName("handleCustomerNotFoundException should create ErrorDTO with NOT_FOUND status and resolved message")
    void handleCustomerNotFoundExceptionShouldCreateErrorDTOWithNotFoundStatusAndResolvedMessage() {
        // Given
        String errorCode = "customer.404.001";
        String resolvedMessage = "Customer not found";
        Object[] args = new Long[]{200L};
        CustomerNotFoundException exception = new CustomerNotFoundException(errorCode, args);

        // Mock the message source to return the resolved message when given the error code and args
        when(messageSource.getMessage(eq(errorCode), eq(args), eq(errorCode), any(Locale.class)))
                .thenReturn(resolvedMessage);

        // When
        final ErrorDTO result = validationExceptionHandler.handleCustomerNotFound(exception);

        // Then
        assertNotNull(result);
        assertEquals(resolvedMessage, result.getMessage());
        assertEquals("NOT_FOUND", result.getName());
        assertNull(result.getViolations());
        assertNotNull(result.getTimestamp());
    }
}