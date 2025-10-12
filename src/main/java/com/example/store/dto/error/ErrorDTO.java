package com.example.store.dto.error;

import com.example.store.config.adapter.ZonedDateTimeBiSerializer;
import com.google.gson.annotations.JsonAdapter;

import java.time.ZonedDateTime;
import java.util.List;


public record ErrorDTO(
        String name,
        String message,
        List<ViolationDTO> violations,
        @JsonAdapter(ZonedDateTimeBiSerializer.class)
        ZonedDateTime timestamp) {
}