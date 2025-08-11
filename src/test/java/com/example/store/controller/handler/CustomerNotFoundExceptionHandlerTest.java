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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - CustomerNotFoundException Handler")
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
        CustomerNotFoundException exception = new CustomerNotFoundException(errorCode);

        // Mock the message source to return the resolved message when given the error code
        when(messageSource.getMessage(eq(errorCode), isNull(), eq(errorCode), any(Locale.class)))
                .thenReturn(resolvedMessage);

        // When
        ErrorDTO result = validationExceptionHandler.handleCustomerNotFoundException(exception);

        // Then
        assertNotNull(result);
        assertEquals(resolvedMessage, result.getMessage());
        assertEquals("NOT_FOUND", result.getName());
        assertNull(result.getViolations());
        assertNotNull(result.getTimestamp());
    }
}