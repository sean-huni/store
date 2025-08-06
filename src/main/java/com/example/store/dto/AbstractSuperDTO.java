package com.example.store.dto;

import com.google.gson.annotations.JsonAdapter;
import com.example.store.config.adapter.ZonedDateTimeBiSerializer;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public abstract class AbstractSuperDTO {

    private Long id;
    @JsonAdapter(ZonedDateTimeBiSerializer.class)
    private ZonedDateTime created;
    @JsonAdapter(ZonedDateTimeBiSerializer.class)
    private ZonedDateTime updated;

    @PrePersist
    public void prePersist() {
        this.created = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updated = ZonedDateTime.now();
    }
}
