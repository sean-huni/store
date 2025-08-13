package com.example.store.persistence.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@DisplayName("AbstractSuperEntity - {Unit}")
class AbstractSuperEntityTest {

    // Concrete subclass for testing the abstract class
    static class TestEntity extends AbstractSuperEntity {
        private String name;
        
        public TestEntity() {
        }
        
        public TestEntity(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    @Test
    @DisplayName("Should set created timestamp on prePersist")
    void shouldSetCreatedTimestampOnPrePersist() {
        // Given
        TestEntity entity = new TestEntity("Test Entity");
        assertNull(entity.getCreated());
        
        // When
        entity.prePersist();
        
        // Then
        assertNotNull(entity.getCreated());
        assertTrue(entity.getCreated().isBefore(ZonedDateTime.now().plusSeconds(1)));
        assertTrue(entity.getCreated().isAfter(ZonedDateTime.now().minusMinutes(1)));
    }
    
    @Test
    @DisplayName("Should set updated timestamp on preUpdate")
    void shouldSetUpdatedTimestampOnPreUpdate() {
        // Given
        TestEntity entity = new TestEntity("Test Entity");
        assertNull(entity.getUpdated());
        
        // When
        entity.preUpdate();
        
        // Then
        assertNotNull(entity.getUpdated());
        assertTrue(entity.getUpdated().isBefore(ZonedDateTime.now().plusSeconds(1)));
        assertTrue(entity.getUpdated().isAfter(ZonedDateTime.now().minusMinutes(1)));
    }
    
    @Test
    @DisplayName("Should get and set id")
    void shouldGetAndSetId() {
        // Given
        TestEntity entity = new TestEntity("Test Entity");
        
        // When
        entity.setId(123L);
        
        // Then
        assertEquals(123L, entity.getId());
    }
    
    @Test
    @DisplayName("Should get and set created timestamp")
    void shouldGetAndSetCreatedTimestamp() {
        // Given
        TestEntity entity = new TestEntity("Test Entity");
        ZonedDateTime now = ZonedDateTime.now();
        
        // When
        entity.setCreated(now);
        
        // Then
        assertEquals(now, entity.getCreated());
    }
    
    @Test
    @DisplayName("Should get and set updated timestamp")
    void shouldGetAndSetUpdatedTimestamp() {
        // Given
        TestEntity entity = new TestEntity("Test Entity");
        ZonedDateTime now = ZonedDateTime.now();
        
        // When
        entity.setUpdated(now);
        
        // Then
        assertEquals(now, entity.getUpdated());
    }
}