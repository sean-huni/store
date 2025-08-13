package com.example.store.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit")
@DisplayName("AppConstant - {Unit}")
class AppConstantTest {

    @Test
    @DisplayName("GLOBAL_ERROR_MSG_PREFIX should have the correct value")
    void globalErrorMsgPrefixShouldHaveCorrectValue() {
        // Given
        String expectedValue = "global.400";

        // When
        String actualValue = AppConstant.GLOBAL_ERROR_MSG_PREFIX;

        // Then
        assertEquals(expectedValue, actualValue, "GLOBAL_ERROR_MSG_PREFIX should be 'global.400'");
    }

    @Test
    @DisplayName("AppConstant constructor should be callable")
    void appConstantConstructorShouldBeCallable() {
        // When
        AppConstant appConstant = new AppConstant();

        // Then
        assertNotNull(appConstant, "AppConstant instance should not be null");
    }
}