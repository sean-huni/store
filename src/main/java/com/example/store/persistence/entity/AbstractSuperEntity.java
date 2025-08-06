package com.example.store.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@MappedSuperclass
public class AbstractSuperEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false, columnDefinition = "BigSerial")
    private Long id;
    private ZonedDateTime created;
    private ZonedDateTime updated;

    @PrePersist
    public void prePersist() {
        created = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = ZonedDateTime.now();
    }
}
