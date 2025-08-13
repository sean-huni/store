package com.example.store.config.adapter;

import com.example.store.exception.LocalizedJsonParseException;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;

@Log4j2
@Component
public class ZonedDateTimeBiSerializer implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    /**
     * Deserializes a JSON element into a ZonedDateTime object.
     *
     * @param jsonElement The JSON to deserialize.
     * @param type        The type of the Object to deserialize to.
     * @return The deserialized ZonedDateTime object.
     * @throws JsonParseException If the JSON element cannot be parsed into a ZonedDateTime.
     */
    @Override
    public ZonedDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        if (jsonElement == null) {
            return null;
        }

        try {
            String dateTimeStr = jsonElement.getAsString();
            return dateTimeStr != null && !dateTimeStr.isBlank()
                    ? ZonedDateTime.parse(dateTimeStr, FORMATTER)
                    : null;
        } catch (Exception e) {
            log.error("Error parsing ZonedDateTime: {}", e.getMessage(), e);
            // Only carry a message key and fallback message; let a higher layer localize it.
            String valueStr = null;
            try {
                valueStr = jsonElement.getAsString();
            } catch (Exception ex) {
                // Ignore this exception, we'll just use null for the value
                log.debug("Could not get string value from JsonElement: {}", ex.getMessage());
                throw new LocalizedJsonParseException(
                        "global.400.011",
                        new Object[]{null},
                        "Error parsing ZonedDateTime",
                        ex
                );
            }

            throw new LocalizedJsonParseException(
                    "global.400.011",
                    new Object[]{valueStr},
                    "Error parsing ZonedDateTime",
                    e
            );
        }
    }

    /**
     * Serializes a ZonedDateTime object into its JSON representation.
     *
     * @param dateTime The ZonedDateTime object to serialize.
     * @param type     The type of the object to serialize.
     * @return The JSON element representing the ZonedDateTime.
     */
    @Override
    public JsonElement serialize(ZonedDateTime dateTime, Type type, JsonSerializationContext ctx) {
        return nonNull(dateTime) ? ctx.serialize(dateTime.format(FORMATTER)) : null;
    }
}
