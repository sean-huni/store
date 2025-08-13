package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("MethodArgumentTypeMismatchExceptionHandler - {Unit}")
@ExtendWith(MockitoExtension.class)
class MethodArgumentTypeMismatchExceptionHandlerTest {

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
    @DisplayName("handleTypeMismatch should create ErrorDTO with generic message for non-sortDir parameters")
    void handleTypeMismatchShouldCreateErrorDTOWithGenericMessageForNonSortDirParameters() {
        // Given
        String paramName = "page";
        String paramValue = "invalid";

        // Create a mock MethodArgumentTypeMismatchException for a non-sortDir parameter
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn(paramName);
        when(exception.getValue()).thenReturn(paramValue);

        // Mock the message source to return the expected message for the generic parameter type mismatch
        String expectedMessage = "Parameter '" + paramName + "' has invalid value: '" + paramValue + "'";
        when(messageSource.getMessage(eq("global.400.010"), any(), any(), any(Locale.class)))
                .thenReturn(expectedMessage);

        // When
        ErrorDTO result = validationExceptionHandler.handleTypeMismatch(exception);

        // Then
        assertNotNull(result);
        assertEquals("Parameter 'page' has invalid value: 'invalid'", result.getMessage());
        assertEquals("BAD_REQUEST", result.getName());
        assertNull(result.getViolations());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("handleMethodArgumentTypeMismatchException should create ErrorDTO with specific message for sortDir parameter")
    void handleMethodArgumentTypeMismatchExceptionShouldCreateErrorDTOWithSpecificMessageForSortDirParameter() {
        // Given
        String paramName = "sortDir";
        String paramValue = "invalid";

        // Create a mock MethodArgumentTypeMismatchException for sortDir parameter
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn(paramName);
        when(exception.getValue()).thenReturn(paramValue);

        // Mock the message source to return the expected message for the generic parameter type mismatch
        String expectedMessage = "Parameter '" + paramName + "' has invalid value: '" + paramValue + "'";
        when(messageSource.getMessage(eq("global.400.010"), any(), any(), any(Locale.class)))
                .thenReturn(expectedMessage);

        // When
        ErrorDTO result = validationExceptionHandler.handleTypeMismatch(exception);

        // Then
        assertNotNull(result);
        assertEquals("Parameter 'sortDir' has invalid value: 'invalid'", result.getMessage());
        assertEquals("BAD_REQUEST", result.getName());
        assertNull(result.getViolations());
        assertNotNull(result.getTimestamp());
    }
}