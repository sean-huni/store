package com.example.store.config;

import com.example.store.config.adapter.ZonedDateTimeBiSerializer;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.error.ErrorDTO;
import com.example.store.dto.error.ViolationDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit")
class GsonConfigTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        // Create Gson instance manually instead of using Spring context
        gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeBiSerializer())
                .create();
    }

    @Test
    void testZonedDateTimeSerialization() {
        // Given
        final ZonedDateTime now = ZonedDateTime.now();
        final List<ViolationDTO> violations = Collections.singletonList(new ViolationDTO("field1", "value1", "Error message"));
        final ErrorDTO errorDTO = new ErrorDTO("Error", "An error occurred", violations, now);

        // When
        final String json = gson.toJson(errorDTO);
        System.out.println("Serialized ErrorDTO JSON: " + json);
        final ErrorDTO deserializedErrorDTO = gson.fromJson(json, ErrorDTO.class);

        // Then
        assertNotNull(json);
        assertNotNull(deserializedErrorDTO);
        assertEquals(errorDTO.getName(), deserializedErrorDTO.getName());
        assertEquals(errorDTO.getMessage(), deserializedErrorDTO.getMessage());
        assertEquals(errorDTO.getViolations().size(), deserializedErrorDTO.getViolations().size());

        // Compare the timestamps - they should be equal after serialization and deserialization
        // We're comparing the string representation because ZonedDateTime equality can be tricky
        assertEquals(
                errorDTO.getTimestamp().format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                deserializedErrorDTO.getTimestamp().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );
    }

    @Test
    void testAbstractSuperDTOSerialization() {
        // Given
        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setName("Test Customer");
        customerDTO.setOrders(new HashSet<>());

        ZonedDateTime now = ZonedDateTime.now();
        customerDTO.setCreated(now);
        customerDTO.setUpdated(now);

        // When
        String json = gson.toJson(customerDTO);
        System.out.println("Serialized CustomerDTO JSON: " + json);
        CustomerDTO deserializedCustomerDTO = gson.fromJson(json, CustomerDTO.class);

        // Then
        assertNotNull(json);
        assertNotNull(deserializedCustomerDTO);
        assertEquals(customerDTO.getId(), deserializedCustomerDTO.getId());
        assertEquals(customerDTO.getName(), deserializedCustomerDTO.getName());

        // Compare the timestamps - they should be equal after serialization and deserialization
        assertEquals(
                customerDTO.getCreated().format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                deserializedCustomerDTO.getCreated().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );
        assertEquals(
                customerDTO.getUpdated().format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                deserializedCustomerDTO.getUpdated().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
        );
    }
}