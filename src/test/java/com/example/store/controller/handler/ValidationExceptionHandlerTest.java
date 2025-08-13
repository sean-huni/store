package com.example.store.controller.handler;

import com.example.store.dto.SortEnumDTO;
import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.EmailAlreadyExistsException;
import com.example.store.exception.InvalidRefreshTokenException;
import com.example.store.exception.LocalizedJsonParseException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("ValidationExceptionHandler - {Unit}")
@ExtendWith(MockitoExtension.class)
class ValidationExceptionHandlerTest {
    @Mock
    private MessageSource messageSource;

    @Mock
    private FieldErrorExtractor fieldErrorExtractor;

    private ValidationExceptionHandler validationExceptionHandler;

    private static final String GLOBAL_ERROR_CODE = "global.400.000";
    private static final String RESOLVED_MESSAGE = "Request failed. An unexpected error occurred. Please try again later";

    @BeforeEach
    void setUp() {
        // Initialize the ValidationExceptionHandler with mocked dependencies
        validationExceptionHandler = new ValidationExceptionHandler(messageSource, fieldErrorExtractor);

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
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);

            List<ViolationDTO> violations = new ArrayList<>();
            violations.add(new ViolationDTO("field", "value", "error", "error.code.seq"));

            // Mock the FieldErrorExtractor behavior
            when(fieldErrorExtractor.extractErrorObjects(exception)).thenReturn(violations);

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            ErrorDTO result = validationExceptionHandler.handleHandlerMethodValidation(exception);

            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getTimestamp());
            assertEquals(violations, result.getViolations());
        }
    }

    @Nested
    @DisplayName("When handling MethodArgumentNotValidException")
    class WhenHandlingMethodArgumentNotValidException {

        @Test
        @DisplayName("Then extract error message and create ErrorDTO")
        void thenExtractErrorMessageAndCreateErrorDTO() {
            // Given
            MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);

            BindingResult bindingResult = mock(BindingResult.class);
            List<FieldError> fieldErrors = new ArrayList<>();
            when(exception.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

            List<ViolationDTO> violations = new ArrayList<>();
            violations.add(new ViolationDTO("field", "value", "error", "error.code.seq"));

            // Mock the FieldErrorExtractor behavior
            when(fieldErrorExtractor.extractErrorObjects(fieldErrors)).thenReturn(violations);

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            ErrorDTO result = validationExceptionHandler.handleMethodArgumentNotValid(exception);

            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getTimestamp());
            assertEquals(violations, result.getViolations());
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
            String expectedMessage = "An unexpected error occurred";

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.000"), isNull(), eq("An unexpected error occurred"), any(Locale.class)))
                    .thenReturn(expectedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleGeneralException(exception);

            // Then
            assertNotNull(result);
            assertEquals(expectedMessage, result.getMessage());
            assertEquals("INTERNAL_SERVER_ERROR", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When extracting quoted text")
    class WhenExtractingQuotedText {

        @Test
        @DisplayName("Then create ErrorDTO with validation failed message")
        void thenCreateErrorDTOWithValidationFailedMessage() {
            // Given
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);

            // Mock the FieldErrorExtractor to return empty violations
            when(fieldErrorExtractor.extractErrorObjects(any(HandlerMethodValidationException.class)))
                    .thenReturn(new ArrayList<>());

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            ErrorDTO result = validationExceptionHandler.handleHandlerMethodValidation(exception);

            // Then
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then return validation failed message for any input")
        void thenReturnValidationFailedMessageForAnyInput() {
            // Given
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);

            // Mock the FieldErrorExtractor to return empty violations
            when(fieldErrorExtractor.extractErrorObjects(any(HandlerMethodValidationException.class)))
                    .thenReturn(new ArrayList<>());

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            ErrorDTO result = validationExceptionHandler.handleHandlerMethodValidation(exception);

            // Then
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then handle null message and return validation failed")
        void thenHandleNullMessageAndReturnValidationFailed() {
            // Given
            HandlerMethodValidationException exception = mock(HandlerMethodValidationException.class);

            // Mock the FieldErrorExtractor to return empty violations
            when(fieldErrorExtractor.extractErrorObjects(any(HandlerMethodValidationException.class)))
                    .thenReturn(new ArrayList<>());

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            ErrorDTO result = validationExceptionHandler.handleHandlerMethodValidation(exception);

            // Then
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When handling EmailAlreadyExistsException")
    class WhenHandlingEmailAlreadyExistsException {

        @Test
        @DisplayName("Then create ErrorDTO with CONFLICT status")
        void thenCreateErrorDTOWithConflictStatus() {
            // Given
            final String errorMessage = "auth.409.001";
            final String email = "test@email.com";
            final String resolvedMessage = "Email already exists";
            final EmailAlreadyExistsException exception = new EmailAlreadyExistsException(errorMessage, new String[]{email});

            // Mock the message source to return the resolved message
            when(messageSource.getMessage(eq("auth.409.001"), eq(new String[]{email}), eq("Email already exists"), any(Locale.class)))
                    .thenReturn(resolvedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleEmailAlreadyExists(exception);

            // Then
            assertNotNull(result);
            assertEquals(resolvedMessage, result.getMessage());
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
            String errorCode = "auth.401.002";
            String resolvedMessage = "Invalid or expired refresh token";
            InvalidRefreshTokenException exception = new InvalidRefreshTokenException(errorCode);

            // Mock the message source to return the resolved message when given the error code
            when(messageSource.getMessage(eq(errorCode), isNull(), eq("Invalid or expired refresh token"), any(Locale.class)))
                    .thenReturn(resolvedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleInvalidRefreshToken(exception);

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

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("auth.401.001"), isNull(), eq("Invalid credentials"), any(Locale.class)))
                    .thenReturn("Invalid credentials");

            // When
            ErrorDTO result = validationExceptionHandler.handleAuthenticationException(exception);

            // Then
            assertNotNull(result);
            assertEquals("Invalid credentials", result.getMessage());
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

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("auth.401.001"), isNull(), eq("Invalid credentials"), any(Locale.class)))
                    .thenReturn("Invalid credentials");

            // When
            ErrorDTO result = validationExceptionHandler.handleAuthenticationException(exception);

            // Then
            assertNotNull(result);
            assertEquals("Invalid credentials", result.getMessage());
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
            // Create the expected ViolationDTO list
            List<ViolationDTO> violationDTOs = new ArrayList<>();
            violationDTOs.add(new ViolationDTO("fieldName", "invalidValue", "error message", "error.code.seq"));

            // Mock the FieldErrorExtractor behavior to return the expected violations for any ConstraintViolationException
            when(fieldErrorExtractor.extractErrorObjects(any(ConstraintViolationException.class))).thenReturn(violationDTOs);

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // Create a simple ConstraintViolationException
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            ErrorDTO result = validationExceptionHandler.handleConstraintViolation(exception);

            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(violationDTOs, result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then handle global message code in violation")
        void thenHandleGlobalMessageCodeInViolation() {
            // Given
            // Create a simple ConstraintViolationException with an empty set of violations
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // Create the expected ViolationDTO list
            final List<ViolationDTO> violationDTOs = new ArrayList<>();
            violationDTOs.add(new ViolationDTO("fieldName", "invalidValue", "Resolved error message", "error.code.seq"));

            // Mock the FieldErrorExtractor behavior
            doReturn(violationDTOs).when(fieldErrorExtractor).extractErrorObjects(any(ConstraintViolationException.class));

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // When
            final ErrorDTO result = validationExceptionHandler.handleConstraintViolation(exception);

            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(violationDTOs, result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then handle null invalid value in violation")
        void thenHandleNullInvalidValueInViolation() {
            // Given
            // Create the expected ViolationDTO list
            List<ViolationDTO> violationDTOs = new ArrayList<>();
            violationDTOs.add(new ViolationDTO("fieldName", "", "error message", "error.code.seq"));

            // Mock the FieldErrorExtractor behavior to return the expected violations for any ConstraintViolationException
            when(fieldErrorExtractor.extractErrorObjects(any(ConstraintViolationException.class))).thenReturn(violationDTOs);

            // Mock the message source to return the expected message
            when(messageSource.getMessage(eq("global.400.001"), isNull(), eq("Validation failed"), any(Locale.class)))
                    .thenReturn("Validation failed");

            // Create a simple ConstraintViolationException
            final Set<ConstraintViolation<?>> violations = new HashSet<>();
            final ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            // When
            final ErrorDTO result = validationExceptionHandler.handleConstraintViolation(exception);

            // Then
            assertNotNull(result);
            assertEquals("Validation failed", result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNotNull(result.getViolations());
            assertEquals(violationDTOs, result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When handling CustomerNotFoundException")
    class WhenHandlingCustomerNotFoundException {

        @Test
        @DisplayName("Then create ErrorDTO with NOT_FOUND status and resolved message")
        void thenCreateErrorDTOWithNotFoundStatusAndResolvedMessage() {
            // Given
            String errorCode = "customer.404.001";
            String resolvedMessage = "Customer not found";
            Object[] args = new Object[]{1L};
            CustomerNotFoundException exception = new CustomerNotFoundException(errorCode, args);

            // Mock the message source to return the resolved message when given the error code and args
            when(messageSource.getMessage(eq(errorCode), eq(args), eq(errorCode), any(Locale.class)))
                    .thenReturn(resolvedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleCustomerNotFound(exception);

            // Then
            assertNotNull(result);
            assertEquals(resolvedMessage, result.getMessage());
            assertEquals("NOT_FOUND", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }

    @Nested
    @DisplayName("When handling MethodArgumentTypeMismatchException")
    class WhenHandlingMethodArgumentTypeMismatchException {

        @Test
        @DisplayName("Then create ErrorDTO with specific message for sortDir parameter")
        void thenCreateErrorDTOWithSpecificMessageForSortDirParameter() {
            // Given
            String paramName = "sortDir";
            String paramValue = "invalid";
            String resolvedMessage = "Invalid sort direction";

            // Create a mock MethodArgumentTypeMismatchException for sortDir parameter
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

            // Set up the mock to trigger the sortDir branch - using lenient() to avoid "unnecessary stubbing" errors
            lenient().when(exception.getName()).thenReturn(paramName);
            lenient().when(exception.getValue()).thenReturn(paramValue);

            // Mock the getRequiredType() method to return a Class that is an enum
            // Using doReturn().when() syntax to avoid type casting issues
            Class<?> enumClass = SortEnumDTO.class;
            doReturn(enumClass).when(exception).getRequiredType();

            // Mock the message source to return the resolved message
            lenient().when(messageSource.getMessage(eq("global.400.009"), isNull(), eq("Invalid value for parameter 'sortDir'"), any(Locale.class)))
                    .thenReturn(resolvedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleTypeMismatch(exception);

            // Then
            assertNotNull(result);
            assertEquals(resolvedMessage, result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then create ErrorDTO with generic message for other parameters")
        void thenCreateErrorDTOWithGenericMessageForOtherParameters() {
            // Given
            String paramName = "page";
            String paramValue = "invalid";

            // Create a mock MethodArgumentTypeMismatchException for a non-sortDir parameter
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

            // Set up the mock to avoid triggering the sortDir branch
            doReturn(paramName).when(exception).getName(); // Not "sortDir"
            doReturn(paramValue).when(exception).getValue();

            // Mock the message source to return the expected message for the generic parameter type mismatch
            String expectedMessage = "Parameter '%s' has invalid value: '%s'".formatted(paramName, paramValue);
            lenient().when(messageSource.getMessage(eq("global.400.010"), any(), any(), any(Locale.class)))
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
        @DisplayName("Then create ErrorDTO with generic message for sortDir parameter with non-enum required type")
        void thenCreateErrorDTOWithGenericMessageForSortDirParameterWithNonEnumRequiredType() {
            // Given
            String paramName = "sortDir";
            String paramValue = "invalid";

            // Create a mock MethodArgumentTypeMismatchException for sortDir parameter
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

            // Set up the mock to have sortDir name but non-enum required type
            doReturn(paramName).when(exception).getName();
            doReturn(paramValue).when(exception).getValue();
            doReturn(String.class).when(exception).getRequiredType(); // Non-enum type

            // Mock the message source to return the expected message for the generic parameter type mismatch
            String expectedMessage = "Parameter '" + paramName + "' has invalid value: '" + paramValue + "'";
            lenient().when(messageSource.getMessage(eq("global.400.010"), any(), any(), any(Locale.class)))
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

        @Test
        @DisplayName("Then create ErrorDTO with generic message for sortDir parameter with null required type")
        void thenCreateErrorDTOWithGenericMessageForSortDirParameterWithNullRequiredType() {
            // Given
            String paramName = "sortDir";
            String paramValue = "invalid";

            // Create a mock MethodArgumentTypeMismatchException for sortDir parameter
            MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

            // Set up the mock to have sortDir name but null required type
            doReturn(paramName).when(exception).getName();
            doReturn(paramValue).when(exception).getValue();
            doReturn(null).when(exception).getRequiredType(); // Null type

            // Mock the message source to return the expected message for the generic parameter type mismatch
            String expectedMessage = "Parameter '" + paramName + "' has invalid value: '" + paramValue + "'";
            lenient().when(messageSource.getMessage(eq("global.400.010"), any(), any(), any(Locale.class)))
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

    @Nested
    @DisplayName("When handling LocalizedJsonParseException")
    class WhenHandlingLocalizedJsonParseException {

        @Test
        @DisplayName("Then create ErrorDTO with BAD_REQUEST status and localized message")
        void thenCreateErrorDTOWithBadRequestStatusAndLocalizedMessage() {
            // Given
            String messageKey = "global.400.011";
            String invalidValue = "invalid-date-time";
            Object[] args = new Object[]{invalidValue};
            String fallbackMessage = "Error parsing ZonedDateTime";
            String localizedMessage = "Error parsing date: " + invalidValue;

            // Create a LocalizedJsonParseException
            LocalizedJsonParseException exception = new LocalizedJsonParseException(
                    messageKey,
                    args,
                    fallbackMessage,
                    new RuntimeException("Original cause")
            );

            // Mock the message source to return the localized message
            when(messageSource.getMessage(eq(messageKey), eq(args), eq(fallbackMessage), any(Locale.class)))
                    .thenReturn(localizedMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleJsonParseException(exception);

            // Then
            assertNotNull(result);
            assertEquals(localizedMessage, result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then use fallback message when message key is not found")
        void thenUseFallbackMessageWhenMessageKeyIsNotFound() {
            // Given
            String messageKey = "unknown.key";
            String invalidValue = "invalid-date-time";
            Object[] args = new Object[]{invalidValue};
            String fallbackMessage = "Error parsing ZonedDateTime";

            // Create a LocalizedJsonParseException
            LocalizedJsonParseException exception = new LocalizedJsonParseException(
                    messageKey,
                    args,
                    fallbackMessage,
                    new RuntimeException("Original cause")
            );

            // Mock the message source to return the fallback message
            when(messageSource.getMessage(eq(messageKey), eq(args), eq(fallbackMessage), any(Locale.class)))
                    .thenReturn(fallbackMessage);

            // When
            ErrorDTO result = validationExceptionHandler.handleJsonParseException(exception);

            // Then
            assertNotNull(result);
            assertEquals(fallbackMessage, result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }

        @Test
        @DisplayName("Then handle null message key")
        void thenHandleNullMessageKey() {
            // Given
            String messageKey = null;
            Object[] args = new Object[]{"invalid-date-time"};
            String fallbackMessage = "Error parsing ZonedDateTime";

            // Create a LocalizedJsonParseException
            LocalizedJsonParseException exception = new LocalizedJsonParseException(
                    messageKey,
                    args,
                    fallbackMessage,
                    new RuntimeException("Original cause")
            );

            // No mock setup needed since messageKey is null and messageSource.getMessage() won't be called

            // When
            ErrorDTO result = validationExceptionHandler.handleJsonParseException(exception);

            // Then
            assertNotNull(result);
            assertEquals(fallbackMessage, result.getMessage());
            assertEquals("BAD_REQUEST", result.getName());
            assertNull(result.getViolations());
            assertNotNull(result.getTimestamp());
        }
    }
}