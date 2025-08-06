package com.example.store.config.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("ZonedDateTimeBiSerializer Tests")
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
    @DisplayName("Should throw JsonParseException when deserializing invalid JSON")
    void shouldThrowJsonParseExceptionWhenDeserializingInvalidJson() {
        // Given
        JsonPrimitive jsonElement = new JsonPrimitive("invalid-date-time");
        
        // When/Then
        assertThrows(JsonParseException.class, () -> {
            serializer.deserialize(jsonElement, type, deserializationContext);
        });
    }
}