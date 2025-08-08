package com.example.store.controller.handler;

import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - ValidationExceptionHandler")
@ExtendWith(MockitoExtension.class)
class ValidationExceptionHandlerTest {
    @Mock
    private MessageSource messageSource;
    @InjectMocks
    private ValidationExceptionHandler validationExceptionHandler;

    private static final String GLOBAL_ERROR_CODE = "global.400.000";
    private static final String RESOLVED_MESSAGE = "Request failed. An unexpected error occurred. Please try again later";

    @BeforeEach
    void setUp() {
        // Use lenient() to avoid "unnecessary stubbing" errors when this mock isn't used in all tests
        lenient().when(messageSource.getMessage(eq(GLOBAL_ERROR_CODE), isNull(), eq(GLOBAL_ERROR_CODE), any(Locale.class)))
                .thenReturn(RESOLVED_MESSAGE);
    }

    @Nested
    @DisplayName("When handling HandlerMethodValidationException")
    class WhenHandlingHandlerMethodValidationException {
        @Test
        @DisplayName("Then extract error message and create ErrorDTO")
        void thenExtractErrorMessageAndCreateErrorDTO() {
            // Given
            String errorMessage = "Validation failed for parameter";
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getMessage()).thenReturn("Error: \"" + errorMessage + "\" occurred");
            
            List<ViolationDTO> violations = new ArrayList<>();
            violations.add(new ViolationDTO("field", "value", "error"));
            
            // Mock the FieldErrorExtractor that will be created inside the handler
            // This is a bit tricky since we can't directly mock it, but we can verify the result
            
            // When
            ErrorDTO result = validationExceptionHandler.invalidInputHandler(exception);

            // Then
            assertNotNull(result);
            assertEquals(errorMessage, result.getMessage());
            assertNull(result.getName());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When handling MethodArgumentNotValidException")
    class WhenHandlingMethodArgumentNotValidException {

        @Test
        @DisplayName("Then extract error message and create ErrorDTO")
        void thenExtractErrorMessageAndCreateErrorDTO() {
            // Given
            String errorMessage = "Validation failed for object";
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
            when(exception.getMessage()).thenReturn("Error: \"" + errorMessage + "\" occurred");
            
            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> fieldErrors = new ArrayList<>();
            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
            
            // When
            ErrorDTO result = validationExceptionHandler.invalidInputHandler(exception);

            // Then
            assertNotNull(result);
            assertEquals(errorMessage, result.getMessage());
            assertNull(result.getName());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When handling general Exception")
    class WhenHandlingGeneralException {

        @Test
        @DisplayName("Then create ErrorDTO with message from MessageSource")
        void thenCreateErrorDTOWithMessageFromMessageSource() {
            // Given
            Exception exception = new RuntimeException("Some unexpected error");
            
            // When
            ErrorDTO result = validationExceptionHandler.handleGeneralException(exception);

            // Then
            assertNotNull(result);
            assertEquals(RESOLVED_MESSAGE, result.getMessage());
            assertEquals("INTERNAL_SERVER_ERROR", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When extracting quoted text")
    class WhenExtractingQuotedText {

        @Test
        @DisplayName("Then extract text inside quotes")
        void thenExtractTextInsideQuotes() {
            // Given
            String message = "Error: \"This is the quoted text\" that we want to extract";
            
            // When - We need to use reflection to test this private method
            // For simplicity, we'll test it indirectly through the public methods
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getMessage()).thenReturn(message);
            
            // When
            ErrorDTO result = validationExceptionHandler.invalidInputHandler(exception);
            
            // Then
            assertEquals("This is the quoted text", result.getMessage());
        }

        @Test
        @DisplayName("Then return original message if no quotes")
        void thenReturnOriginalMessageIfNoQuotes() {
            // Given
            String message = "Error message without quotes";
            
            // When - We need to use reflection to test this private method
            // For simplicity, we'll test it indirectly through the public methods
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getMessage()).thenReturn(message);
            
            // When
            ErrorDTO result = validationExceptionHandler.invalidInputHandler(exception);
            
            // Then
            assertEquals(message, result.getMessage());
        }

        @Test
        @DisplayName("Then handle null message")
        void thenHandleNullMessage() {
            // Given
            String message = null;
            
            // When - We need to use reflection to test this private method
            // For simplicity, we'll test it indirectly through the public methods
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            when(exception.getMessage()).thenReturn(message);
            
            // When
            ErrorDTO result = validationExceptionHandler.invalidInputHandler(exception);
            
            // Then
            assertNull(result.getMessage());
        }
    }
    
    @Nested
    @DisplayName("When handling EmailAlreadyExistsException")
    class WhenHandlingEmailAlreadyExistsException {
        
        @Test
        @DisplayName("Then create ErrorDTO with CONFLICT status")
        void thenCreateErrorDTOWithConflictStatus() {
            // Given
            String errorMessage = "Email already exists";
            EmailAlreadyExistsException exception = new EmailAlreadyExistsException(errorMessage);
            
            // When
            ErrorDTO result = validationExceptionHandler.handleEmailAlreadyExistsException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals(errorMessage, result.getMessage());
            assertEquals("CONFLICT", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }
    
    @Nested
    @DisplayName("When handling InvalidRefreshTokenException")
    class WhenHandlingInvalidRefreshTokenException {
        
        @Test
        @DisplayName("Then create ErrorDTO with UNAUTHORIZED status")
        void thenCreateErrorDTOWithUnauthorizedStatus() {
            // Given
            String errorCode = "auth.400.006";
            String resolvedMessage = "Invalid refresh token";
            InvalidRefreshTokenException exception = new InvalidRefreshTokenException(errorCode);

            // Mock the message source to return the resolved message when given the error code
            when(messageSource.getMessage(eq(errorCode), isNull(), eq(errorCode), any(Locale.class)))
                    .thenReturn(resolvedMessage);
            
            // When
            ErrorDTO result = validationExceptionHandler.handleInvalidRefreshTokenException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals(resolvedMessage, result.getMessage());
            assertEquals("UNAUTHORIZED", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }
    
    @Nested
    @DisplayName("When handling BadCredentialsException")
    class WhenHandlingBadCredentialsException {
        
        @Test
        @DisplayName("Then create ErrorDTO with UNAUTHORIZED status and custom message")
        void thenCreateErrorDTOWithUnauthorizedStatusAndCustomMessage() {
            // Given
            BadCredentialsException exception = new BadCredentialsException("Original message");
            
            // When
            ErrorDTO result = validationExceptionHandler.handleBadCredentialsException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals("Invalid email or password", result.getMessage()); // Check for hardcoded message
            assertEquals("UNAUTHORIZED", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }
    
    @Nested
    @DisplayName("When handling UsernameNotFoundException")
    class WhenHandlingUsernameNotFoundException {
        
        @Test
        @DisplayName("Then create ErrorDTO with UNAUTHORIZED status and custom message")
        void thenCreateErrorDTOWithUnauthorizedStatusAndCustomMessage() {
            // Given
            UsernameNotFoundException exception = new UsernameNotFoundException("Original message");
            
            // When
            ErrorDTO result = validationExceptionHandler.handleUsernameNotFoundException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals("User not found", result.getMessage()); // Check for hardcoded message
            assertEquals("UNAUTHORIZED", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }
    
    @Nested
    @DisplayName("When handling ConstraintViolationException")
    class WhenHandlingConstraintViolationException {
        
        @Test
        @DisplayName("Then create ErrorDTO with violations")
        void thenCreateErrorDTOWithViolations() {
            // Given
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            
            // Create a mock violation
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("fieldName");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn("invalidValue");
            when(violation.getMessage()).thenReturn("error message");
            
            violations.add(violation);
            
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
            
            // When
            ErrorDTO result = validationExceptionHandler.handleConstraintViolationException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(1, result.getViolations().size());
            assertEquals("fieldName", result.getViolations().get(0).getField());
            assertEquals("invalidValue", result.getViolations().get(0).getRejectedValue());
            assertEquals("error message", result.getViolations().get(0).getErrMsg());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Then handle global message code in violation")
        void thenHandleGlobalMessageCodeInViolation() {
            // Given
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            
            // Create a mock violation with a global message code
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("fieldName");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn("invalidValue");
            when(violation.getMessage()).thenReturn("global.400.123");
            
            // Mock the message source to return a resolved message
            when(messageSource.getMessage(eq("global.400.123"), isNull(), eq("global.400.123"), any(Locale.class)))
                    .thenReturn("Resolved error message");
            
            violations.add(violation);
            
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
            
            // When
            ErrorDTO result = validationExceptionHandler.handleConstraintViolationException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(1, result.getViolations().size());
            assertEquals("fieldName", result.getViolations().get(0).getField());
            assertEquals("invalidValue", result.getViolations().get(0).getRejectedValue());
            assertEquals("Resolved error message", result.getViolations().get(0).getErrMsg());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Then handle null invalid value in violation")
        void thenHandleNullInvalidValueInViolation() {
            // Given
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            
            // Create a mock violation with null invalid value
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("fieldName");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn(null);
            when(violation.getMessage()).thenReturn("error message");
            
            violations.add(violation);
            
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
            
            // When
            ErrorDTO result = validationExceptionHandler.handleConstraintViolationException(exception);
            
            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(1, result.getViolations().size());
            assertEquals("fieldName", result.getViolations().get(0).getField());
            assertEquals("", result.getViolations().get(0).getRejectedValue()); // Empty string for null value
            assertEquals("error message", result.getViolations().get(0).getErrMsg());
            assertNotNull(result.getTimestamp());
        }
    }
}