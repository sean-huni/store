package com.example.store.config.adapter;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;

@Log4j2
public class ZonedDateTimeBiSerializer implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    /**
     * Deserializes a JSON element into a ZonedDateTime object.
     *
     * @param jsonElement                The JSON to deserialize.
     * @param type                       The type of the Object to deserialize to.
     * @param jsonDeserializationContext The deserialization context.
     * @return The deserialized ZonedDateTime object.
     * @throws JsonParseException If the JSON element cannot be parsed into a ZonedDateTime.
     */
    @Override
    public ZonedDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            return nonNull(jsonElement) && !jsonElement.getAsString().isBlank() ? ZonedDateTime.parse(jsonElement.getAsString(), FORMATTER) : null;
        } catch (Exception e) {
            log.error("Error parsing ZonedDateTime: {}", e.getMessage(), e);
            throw new JsonParseException("Error parsing ZonedDateTime", e);
        }
    }

    /**
     * Serializes a ZonedDateTime object into its JSON representation.
     *
     * @param dateTime                 The ZonedDateTime object to serialize.
     * @param type                     The type of the object to serialize.
     * @param jsonSerializationContext The serialization context.
     * @return The JSON element representing the ZonedDateTime.
     */
    @Override
    public JsonElement serialize(ZonedDateTime dateTime, Type type, JsonSerializationContext jsonSerializationContext) {
        return nonNull(dateTime) ? jsonSerializationContext.serialize(dateTime.format(FORMATTER)) : null;
    }
}
