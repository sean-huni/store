package com.example.store.config.adapter;

import com.example.store.exception.LocalizedJsonParseException;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("ZonedDateTimeBiSerializer - {Unit}")
class ZonedDateTimeBiSerializerTest {

    private ZonedDateTimeBiSerializer serializer;

    @Mock
    private JsonSerializationContext serializationContext;

    @Mock
    private JsonDeserializationContext deserializationContext;

    @Mock
    private Type type;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        serializer = new ZonedDateTimeBiSerializer();
    }

    @Test
    @DisplayName("Should serialize ZonedDateTime to JSON")
    void shouldSerializeZonedDateTimeToJson() {
        // Given
        ZonedDateTime dateTime = ZonedDateTime.now();
        String formattedDateTime = dateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        JsonPrimitive expectedJson = new JsonPrimitive(formattedDateTime);

        when(serializationContext.serialize(anyString())).thenReturn(expectedJson);

        // When
        JsonElement result = serializer.serialize(dateTime, type, serializationContext);

        // Then
        assertNotNull(result);
        assertEquals(expectedJson, result);
    }

    @Test
    @DisplayName("Should return null when serializing null ZonedDateTime")
    void shouldReturnNullWhenSerializingNullZonedDateTime() {
        // When
        JsonElement result = serializer.serialize(null, type, serializationContext);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should deserialize JSON to ZonedDateTime")
    void shouldDeserializeJsonToZonedDateTime() {
        // Given
        ZonedDateTime expectedDateTime = ZonedDateTime.now();
        String formattedDateTime = expectedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        JsonPrimitive jsonElement = new JsonPrimitive(formattedDateTime);

        // When
        ZonedDateTime result = serializer.deserialize(jsonElement, type, deserializationContext);

        // Then
        assertNotNull(result);
        assertEquals(expectedDateTime.toEpochSecond(), result.toEpochSecond());
    }

    @Test
    @DisplayName("Should return null when deserializing null JSON element")
    void shouldReturnNullWhenDeserializingNullJsonElement() {
        // When
        ZonedDateTime result = serializer.deserialize(null, type, deserializationContext);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when deserializing empty JSON string")
    void shouldReturnNullWhenDeserializingEmptyJsonString() {
        // Given
        JsonPrimitive jsonElement = new JsonPrimitive("");

        // When
        ZonedDateTime result = serializer.deserialize(jsonElement, type, deserializationContext);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should throw LocalizedJsonParseException when deserializing invalid JSON")
    void shouldThrowLocalizedJsonParseExceptionWhenDeserializingInvalidJson() {
        // Given
        final String invalidValue = "invalid-date-time";
        final JsonPrimitive jsonElement = new JsonPrimitive(invalidValue);

        // When/Then
        final LocalizedJsonParseException exception = assertThrows(LocalizedJsonParseException.class, () ->
                serializer.deserialize(jsonElement, type, deserializationContext));

        // Verify the exception has the correct message key and arguments
        assertEquals("global.400.011", exception.getMessageKey());
        assertNotNull(exception.getArgs());
        assertTrue(exception.getArgs().length > 0);
        assertEquals(invalidValue, exception.getArgs()[0]);
        assertEquals("Error parsing ZonedDateTime", exception.getMessage());
    }

    @Test
    @DisplayName("Should handle exception with null JsonElement")
    void shouldHandleExceptionWithNullJsonElement() {
        // Given
        // Create a mock JsonElement that throws an exception when getAsString is called
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.getAsString()).thenThrow(new NullPointerException("Test exception"));

        // When/Then
        final LocalizedJsonParseException exception = assertThrows(LocalizedJsonParseException.class, () ->
                serializer.deserialize(jsonElement, type, deserializationContext));

        // Verify the exception has the correct message key
        assertEquals("global.400.011", exception.getMessageKey());
        assertNotNull(exception.getArgs());
        // The args array should contain null since jsonElement.getAsString() throws an exception
        assertNotNull(exception.getArgs());
        assertEquals("Error parsing ZonedDateTime", exception.getMessage());
    }

    @Test
    @DisplayName("Should return null when deserializing JSON with null string value")
    void shouldReturnNullWhenDeserializingJsonWithNullStringValue() {
        // Given
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.getAsString()).thenReturn(null);

        // When
        ZonedDateTime result = serializer.deserialize(jsonElement, type, deserializationContext);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when deserializing JSON with blank string value")
    void shouldReturnNullWhenDeserializingJsonWithBlankStringValue() {
        // Given
        JsonElement jsonElement = mock(JsonElement.class);
        when(jsonElement.getAsString()).thenReturn("   "); // Blank but not empty

        // When
        ZonedDateTime result = serializer.deserialize(jsonElement, type, deserializationContext);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle exception during ZonedDateTime parsing")
    void shouldHandleExceptionDuringZonedDateTimeParsing() {
        // Given
        JsonElement jsonElement = mock(JsonElement.class);
        String invalidFormat = "2023-01-01"; // Missing time and zone information
        when(jsonElement.getAsString()).thenReturn(invalidFormat);

        // When/Then
        final LocalizedJsonParseException exception = assertThrows(LocalizedJsonParseException.class, () ->
                serializer.deserialize(jsonElement, type, deserializationContext));

        // Verify the exception has the correct message key and arguments
        assertEquals("global.400.011", exception.getMessageKey());
        assertNotNull(exception.getArgs());
        assertEquals(invalidFormat, exception.getArgs()[0]);
        assertEquals("Error parsing ZonedDateTime", exception.getMessage());
    }

}