package com.example.store.dto.error;

import com.example.store.config.adapter.ZonedDateTimeBiSerializer;
import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Response DTO for API responses, especially for error scenarios.
 * Contains information about the request status, error message,
 * and detailed validation errorDetailDTOS if applicable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDTO {
    private String name;
    private String message;
    private List<ViolationDTO> violations;
    @JsonAdapter(ZonedDateTimeBiSerializer.class)
    private ZonedDateTime timestamp;
}