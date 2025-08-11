package com.example.store;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@Tag("unit")
@DisplayName("StoreApplication Unit Tests")
class StoreApplicationUnitTest {

    @Test
    @DisplayName("StoreApplication can be instantiated")
    void canCreateStoreApplicationInstance() {
        // This test ensures the constructor is covered
        StoreApplication application = new StoreApplication();
        assertNotNull(application);
    }

    @Test
    @DisplayName("Main method exists with correct signature")
    void mainMethodExistsWithCorrectSignature() {
        try {
            // Get the main method using reflection
            Class<?> clazz = StoreApplication.class;
            Method mainMethod = clazz.getMethod("main", String[].class);

            // If we get here, the method exists with the correct signature
            assertNotNull(mainMethod);
        } catch (NoSuchMethodException e) {
            fail("main method not found or has incorrect signature");
        }
    }

    @Test
    @DisplayName("Main method runs without errors when SpringApplication is mocked")
    void mainMethodRunsWithoutErrorsWithMockedSpringApplication() {
        // Use mockito to mock the static SpringApplication.run method
        try (final var mockedSpringApp = mockStatic(SpringApplication.class)) {
            // Configure the mock to return a mock ConfigurableApplicationContext
            mockedSpringApp
                    .when(() -> SpringApplication.run(any(Class.class), any(String[].class)))
                    .thenReturn(null);

            // Create a mock of the arguments
            final String[] args = new String[0];

            // Redirect System.out to avoid console output during tests
            final PrintStream originalOut = System.out;
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outputStream));

            try {
                // Call the main method directly to ensure code coverage
                assertDoesNotThrow(() -> StoreApplication.main(args));
            } finally {
                // Restore the original System.out
                System.setOut(originalOut);
            }

            // Verify that SpringApplication.run was called
            mockedSpringApp.verify(() -> SpringApplication.run(StoreApplication.class, args));
        }
    }
}