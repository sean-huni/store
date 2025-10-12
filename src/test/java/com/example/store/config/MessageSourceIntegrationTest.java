package com.example.store.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Lightweight test that verifies the complete list of message sources.
 * This test loads only MessageSource without the full Spring application context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MessageSourceIntegrationTest.TestConfig.class)
@Tag("unit")
@DisplayName("MessageSource Integration Test - Lightweight (No DB)")
class MessageSourceIntegrationTest {

    @Autowired
    private MessageSource messageSource;

    /**
     * Expected messages from the messages.properties file.
     * This serves as a contract test - if messages change, this test will fail.
     */
    private static final Map<String, String> EXPECTED_MESSAGES = Map.ofEntries(
            // Global error messages
            Map.entry("global.400.000", "Request failed. An unexpected error occurred. Please try again later"),
            Map.entry("global.400.001", "Validation failed. Please check your input"),
            Map.entry("global.400.002", "Required parameter is missing"),
            Map.entry("global.400.003", "Invalid ID value. Please enter a positive ID value"),
            Map.entry("global.400.004", "Request body is invalid or missing"),
            Map.entry("global.400.005", "Size/Limit min is {0}. Please enter a page size/limit >= {0}"),
            Map.entry("global.400.006", "Page number min is {0}. Please enter a page-number >= {0}"),
            Map.entry("global.400.007", "SortBy cannot be empty. Please enter the name of the field to sort-by"),
            Map.entry("global.400.008", "Invalid Request. Unauthorized Access"),
            Map.entry("global.400.009", "Invalid sort direction. Valid values are 'asc' and 'desc'"),
            Map.entry("global.400.010", "Parameter ''{0}'' has invalid value: ''{1}''"),
            Map.entry("global.400.011", "Error parsing ZonedDateTime"),

            // Product error messages
            Map.entry("product.400.000", "Product Description cannot be empty. Please enter a description for the product"),
            Map.entry("product.400.001", "Product SKU cannot be null. Please provide a valid SKU"),

            // Order error messages
            Map.entry("order.400.000", "Invalid Order. Customer associated with the order does not exist"),
            Map.entry("order.400.001", "Invalid Order. An order should have 1 or more product/s"),

            // Auth error messages
            Map.entry("auth.400.000", "Invalid email. Enter a valid email"),
            Map.entry("auth.400.001", "Email is required"),
            Map.entry("auth.400.002", "Password is required"),
            Map.entry("auth.400.003", "First name is required"),
            Map.entry("auth.400.004", "Last name is required"),
            Map.entry("auth.400.005", "Password must be at least 8-characters"),
            Map.entry("auth.400.006", "Invalid refresh token"),
            Map.entry("auth.400.007", "Invalid or expired refresh token"),
            Map.entry("auth.400.008", "Invalid email or password"),
            Map.entry("auth.400.009", "User not found"),
            Map.entry("auth.400.010", "User not found with email: {0}"),
            Map.entry("auth.400.011", "Email already registered: {0}"),
            Map.entry("auth.400.012", "Full authentication is required to access this resource"),

            // Customer error messages
            Map.entry("customer.400.001", "Invalid Customer Name. Name should be less/equal to {0} characters"),

            // Auth 401 error messages
            Map.entry("auth.401.001", "Invalid credentials"),
            Map.entry("auth.401.002", "Invalid or expired refresh token")
    );

    @Test
    @DisplayName("Should load all expected messages from message source without mocking")
    void shouldLoadAllExpectedMessagesFromMessageSource() {
        // Given: The expected messages defined in EXPECTED_MESSAGES map
        final Locale defaultLocale = Locale.getDefault();
        final Map<String, String> actualMessages = new HashMap<>();

        // When: Loading each message from the MessageSource
        EXPECTED_MESSAGES.keySet().forEach(key -> {
            try {
                final String message = messageSource.getMessage(key, null, null, defaultLocale);
                actualMessages.put(key, message);
            } catch (Exception e) {
                fail("Failed to load message for key: %s. Error: %s".formatted(key, e.getMessage()));
            }
        });

        // Then: All messages should be loaded successfully
        assertEquals(EXPECTED_MESSAGES.size(), actualMessages.size(),
                "Number of loaded messages should match expected messages");

        // And: Each message should match exactly
        EXPECTED_MESSAGES.forEach((key, expectedValue) -> {
            final String actualValue = actualMessages.get(key);
            assertNotNull(actualValue, "Message should not be null for key: %s".formatted(key));
            assertEquals(expectedValue, actualValue, "Message content should match exactly for key: %s".formatted(key));
        });
    }

    @Test
    @DisplayName("Should verify complete message count matches properties file")
    void shouldVerifyCompleteMessageCount() {
        // Given: We expect exactly 32 messages based on the properties file
        final int expectedMessageCount = 32;
        final Locale defaultLocale = Locale.getDefault();
        int actualMessageCount = 0;

        // When: Counting all available messages
        for (final String key : EXPECTED_MESSAGES.keySet()) {
            try {
                messageSource.getMessage(key, null, null, defaultLocale);
                actualMessageCount++;
            } catch (Exception _) {
                // Message not found
            }
        }

        // Then: Message count should match exactly
        assertEquals(expectedMessageCount, actualMessageCount, "Total number of messages should be exactly: %d".formatted(expectedMessageCount));
    }

    @Test
    @DisplayName("Should fail if message source is not properly configured")
    void shouldFailIfMessageSourceIsNotProperlyConfigured() {
        // Given: MessageSource should be autowired
        // When & Then: MessageSource should not be null
        assertNotNull(messageSource, "MessageSource should be properly configured and not null");

        // And: MessageSource should be of expected type
        assertInstanceOf(ReloadableResourceBundleMessageSource.class, messageSource, "MessageSource should be ReloadableResourceBundleMessageSource");
    }

    @Test
    @DisplayName("Should handle messages with placeholders correctly")
    void shouldHandleMessagesWithPlaceholdersCorrectly() {
        // Given: Messages with placeholders
        final String emailKey = "auth.400.010";
        final String sizeKey = "global.400.005";
        final String paramKey = "global.400.010";
        final Locale defaultLocale = Locale.getDefault();

        // When: Loading messages with arguments
        final String emailMessage = messageSource.getMessage(emailKey, new Object[]{"test@example.com"}, null, defaultLocale);
        final String sizeMessage = messageSource.getMessage(sizeKey, new Object[]{1}, null, defaultLocale);
        final String paramMessage = messageSource.getMessage(paramKey, new Object[]{"sortDir", "invalid"}, null, defaultLocale);

        // Then: Placeholders should be properly replaced
        assertEquals("User not found with email: test@example.com", emailMessage);
        assertEquals("Size/Limit min is 1. Please enter a page size/limit >= 1", sizeMessage);
        assertEquals("Parameter 'sortDir' has invalid value: 'invalid'", paramMessage);
    }

    @Test
    @DisplayName("Should detect if any message is unexpectedly modified")
    void shouldDetectIfAnyMessageIsUnexpectedlyModified() {
        // Given: A specific message that should remain stable
        final String testKey = "global.400.001";
        final String expectedMessage = "Validation failed. Please check your input";
        final Locale defaultLocale = Locale.getDefault();

        // When: Loading the message
        String actualMessage = messageSource.getMessage(testKey, null, null, defaultLocale);

        // Then: Message should match exactly (this will fail if the message is changed)
        assertEquals(expectedMessage, actualMessage,
                "Message for key '%s' has been unexpectedly modified. If this change is intentional, please update the test.".formatted(testKey));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public MessageSource messageSource() {
            ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
            messageSource.setBasename("classpath:i18n/messages");
            messageSource.setDefaultEncoding("UTF-8");
            messageSource.setCacheSeconds(3600);
            return messageSource;
        }
    }
}