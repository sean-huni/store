package com.example.store.controller.handler;

import com.example.store.dto.error.ViolationDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - FieldErrorExtractor")
@ExtendWith(MockitoExtension.class)
class FieldErrorExtractorTest {

    @Mock
    private MessageSource messageSource;

    private FieldErrorExtractor fieldErrorExtractor;
    private Locale locale;

    @BeforeEach
    void setUp() {
        locale = Locale.getDefault();
        fieldErrorExtractor = new FieldErrorExtractor(messageSource);
    }

    @Nested
    @DisplayName("When extracting error objects from FieldErrors")
    class WhenExtractingErrorObjectsFromFieldErrors {

        @Test
        @DisplayName("Then correctly map field errors to violation DTOs")
        void thenCorrectlyMapFieldErrorsToViolationDTOs() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";
            String defaultMessage = "Name is invalid";
            
            FieldError fieldError = new FieldError("object", field, rejectedValue, 
                    false, null, null, defaultMessage);
            
            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fieldError);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(fieldErrors);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertEquals(defaultMessage, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null rejected value")
        void thenHandleNullRejectedValue() {
            // Given
            String field = "name";
            String defaultMessage = "Name is required";

            FieldError fieldError = new FieldError("object", field, null,
                    false, null, null, defaultMessage);

            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fieldError);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(fieldErrors);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals("", result.get(0).getRejectedValue());
            assertEquals(defaultMessage, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null default message")
        void thenHandleNullDefaultMessage() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";

            FieldError fieldError = new FieldError("object", field, rejectedValue,
                    false, null, null, null);

            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fieldError);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(fieldErrors);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertNull(result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null default message that would start with global.400")
        void thenHandleNullDefaultMessageThatWouldStartWithGlobal400() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";

            // Create a mock FieldError with a null default message
            // but we'll verify that the code would check if it starts with "global.400"
            FieldError fieldError = mock(FieldError.class);
            when(fieldError.getField()).thenReturn(field);
            when(fieldError.getRejectedValue()).thenReturn(rejectedValue);
            when(fieldError.getDefaultMessage()).thenReturn(null);

            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fieldError);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(fieldErrors);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertNull(result.get(0).getErrMsg());

            // Verify that getDefaultMessage was called, which means the code tried to check
            // if it starts with "global.400"
            verify(fieldError, times(1)).getDefaultMessage();
        }

        @Test
        @DisplayName("Then resolve message from MessageSource for global error codes")
        void thenResolveMessageFromMessageSourceForGlobalErrorCodes() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";
            String errorCode = "global.400.001";
            String resolvedMessage = "Validation failed. Please check your input.";
            
            FieldError fieldError = new FieldError("object", field, rejectedValue, 
                    false, null, null, errorCode);
            
            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fieldError);

            when(messageSource.getMessage(eq(errorCode), any(), eq(errorCode), eq(Locale.getDefault())))
                    .thenReturn(resolvedMessage);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(fieldErrors);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertEquals(resolvedMessage, result.get(0).getErrMsg());
            verify(messageSource, times(1)).getMessage(eq(errorCode), any(), eq(errorCode), eq(Locale.getDefault()));
        }
    }

    @Nested
    @DisplayName("When extracting error objects from HandlerMethodValidationException")
    class WhenExtractingErrorObjectsFromHandlerMethodValidationException {

        @Test
        @DisplayName("Then correctly map field errors from exception to violation DTOs")
        void thenCorrectlyMapFieldErrorsFromExceptionToViolationDTOs() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";
            String defaultMessage = "Name is invalid";
            
            FieldError fieldError = new FieldError("object", field, rejectedValue, 
                    false, null, null, defaultMessage);
            
            List<ObjectError> errors = new ArrayList<>();
            errors.add(fieldError);
            
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertEquals(defaultMessage, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null rejected value")
        void thenHandleNullRejectedValue() {
            // Given
            String field = "name";
            String defaultMessage = "Name is required";
            
            FieldError fieldError = new FieldError("object", field, null, 
                    false, null, null, defaultMessage);
            
            List<ObjectError> errors = new ArrayList<>();
            errors.add(fieldError);
            
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals("", result.get(0).getRejectedValue());
            assertEquals(defaultMessage, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle non-field errors")
        void thenHandleNonFieldErrors() {
            // Given
            String defaultMessage = "Validation failed";
            
            ObjectError objectError = new ObjectError("object", defaultMessage);
            
            List<ObjectError> errors = new ArrayList<>();
            errors.add(objectError);
            
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("global", result.get(0).getField());
            assertEquals("null", result.get(0).getRejectedValue());
            assertEquals(defaultMessage, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null default message in non-field errors")
        void thenHandleNullDefaultMessageInNonFieldErrors() {
            // Given
            ObjectError objectError = mock(ObjectError.class);
            when(objectError.getDefaultMessage()).thenReturn(null);
            lenient().when(objectError.getArguments()).thenReturn(null);

            List<ObjectError> errors = new ArrayList<>();
            errors.add(objectError);

            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("global", result.get(0).getField());
            assertEquals("null", result.get(0).getRejectedValue());
            assertNull(result.get(0).getErrMsg());

            // Verify that getDefaultMessage was called
            verify(objectError, times(1)).getDefaultMessage();
        }

        @Test
        @DisplayName("Then handle null default message that would start with global.400 in non-field errors")
        void thenHandleNullDefaultMessageThatWouldStartWithGlobal400InNonFieldErrors() {
            // Given
            ObjectError objectError = mock(ObjectError.class);
            when(objectError.getDefaultMessage()).thenReturn(null);
            lenient().when(objectError.getArguments()).thenReturn(null);

            List<ObjectError> errors = new ArrayList<>();
            errors.add(objectError);

            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("global", result.get(0).getField());
            assertEquals("null", result.get(0).getRejectedValue());
            assertNull(result.get(0).getErrMsg());

            // Verify that getDefaultMessage was called, which means the code tried to check
            // if it starts with "global.400"
            verify(objectError, times(1)).getDefaultMessage();
        }

        @Test
        @DisplayName("Then resolve message from MessageSource for global error codes in non-field errors")
        void thenResolveMessageFromMessageSourceForGlobalErrorCodesInNonFieldErrors() {
            // Given
            String errorCode = "global.400.001";
            String resolvedMessage = "Validation failed. Please check your input.";
            
            ObjectError objectError = new ObjectError("object", errorCode);
            
            List<ObjectError> errors = new ArrayList<>();
            errors.add(objectError);
            
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);
            doReturn(errors).when(exception).getAllErrors();

            when(messageSource.getMessage(eq(errorCode), any(), eq(errorCode), eq(Locale.getDefault())))
                    .thenReturn(resolvedMessage);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("global", result.get(0).getField());
            assertEquals("null", result.get(0).getRejectedValue());
            assertEquals(resolvedMessage, result.get(0).getErrMsg());
            verify(messageSource, times(1)).getMessage(eq(errorCode), any(), eq(errorCode), eq(Locale.getDefault()));
        }
    }

    @Nested
    @DisplayName("When extracting error objects from ConstraintViolationException")
    class WhenExtractingErrorObjectsFromConstraintViolationException {

        @Test
        @DisplayName("Then correctly map constraint violations to violation DTOs")
        void thenCorrectlyMapConstraintViolationsToViolationDTOs() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";
            String message = "Name is invalid";

            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(field);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn(rejectedValue);
            when(violation.getMessage()).thenReturn(message);

            Set<ConstraintViolation<?>> violations = new HashSet<>();
            violations.add(violation);

            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertEquals(message, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then handle null invalid value")
        void thenHandleNullInvalidValue() {
            // Given
            String field = "name";
            String message = "Name is required";

            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(field);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn(null);
            when(violation.getMessage()).thenReturn(message);

            Set<ConstraintViolation<?>> violations = new HashSet<>();
            violations.add(violation);

            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals("", result.get(0).getRejectedValue());
            assertEquals(message, result.get(0).getErrMsg());
        }

        @Test
        @DisplayName("Then resolve message from MessageSource for global error codes")
        void thenResolveMessageFromMessageSourceForGlobalErrorCodes() {
            // Given
            String field = "name";
            String rejectedValue = "invalid";
            String errorCode = "global.400.001";
            String resolvedMessage = "Validation failed. Please check your input.";

            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn(field);
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getInvalidValue()).thenReturn(rejectedValue);
            when(violation.getMessage()).thenReturn(errorCode);

            Set<ConstraintViolation<?>> violations = new HashSet<>();
            violations.add(violation);

            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            when(messageSource.getMessage(eq(errorCode), isNull(), eq(errorCode), eq(Locale.getDefault())))
                    .thenReturn(resolvedMessage);

            // When
            List<ViolationDTO> result = fieldErrorExtractor.extractErrorObjects(exception);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(field, result.get(0).getField());
            assertEquals(rejectedValue, result.get(0).getRejectedValue());
            assertEquals(resolvedMessage, result.get(0).getErrMsg());
            verify(messageSource, times(1)).getMessage(eq(errorCode), isNull(), eq(errorCode), eq(Locale.getDefault()));
        }
    }
}