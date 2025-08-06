package com.example.store.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("AbstractSuperDTO Tests")
class AbstractSuperDTOTest {

    // Concrete subclass for testing the abstract class
    static class TestDTO extends AbstractSuperDTO {
        private String name;
        
        public TestDTO() {
        }
        
        public TestDTO(String name) {
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
        TestDTO dto = new TestDTO("Test DTO");
        assertNull(dto.getCreated());
        
        // When
        dto.prePersist();
        
        // Then
        assertNotNull(dto.getCreated());
        assertTrue(dto.getCreated().isBefore(ZonedDateTime.now().plusSeconds(1)));
        assertTrue(dto.getCreated().isAfter(ZonedDateTime.now().minusMinutes(1)));
    }
    
    @Test
    @DisplayName("Should set updated timestamp on preUpdate")
    void shouldSetUpdatedTimestampOnPreUpdate() {
        // Given
        TestDTO dto = new TestDTO("Test DTO");
        assertNull(dto.getUpdated());
        
        // When
        dto.preUpdate();
        
        // Then
        assertNotNull(dto.getUpdated());
        assertTrue(dto.getUpdated().isBefore(ZonedDateTime.now().plusSeconds(1)));
        assertTrue(dto.getUpdated().isAfter(ZonedDateTime.now().minusMinutes(1)));
    }
    
    @Test
    @DisplayName("Should get and set id")
    void shouldGetAndSetId() {
        // Given
        TestDTO dto = new TestDTO("Test DTO");
        
        // When
        dto.setId(123L);
        
        // Then
        assertEquals(123L, dto.getId());
    }
    
    @Test
    @DisplayName("Should get and set created timestamp")
    void shouldGetAndSetCreatedTimestamp() {
        // Given
        TestDTO dto = new TestDTO("Test DTO");
        ZonedDateTime now = ZonedDateTime.now();
        
        // When
        dto.setCreated(now);
        
        // Then
        assertEquals(now, dto.getCreated());
    }
    
    @Test
    @DisplayName("Should get and set updated timestamp")
    void shouldGetAndSetUpdatedTimestamp() {
        // Given
        TestDTO dto = new TestDTO("Test DTO");
        ZonedDateTime now = ZonedDateTime.now();
        
        // When
        dto.setUpdated(now);
        
        // Then
        assertEquals(now, dto.getUpdated());
    }
    
    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        TestDTO dto1 = new TestDTO("Test DTO");
        dto1.setId(1L);
        
        TestDTO dto2 = new TestDTO("Test DTO");
        dto2.setId(1L);
        
        TestDTO dto3 = new TestDTO("Test DTO");
        dto3.setId(2L);
        
        // Then
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
        
        assertNotEquals(dto1, null);
        assertNotEquals(dto1, new Object());
    }
    
    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
        // Given
        TestDTO dto = new TestDTO("Test DTO");
        dto.setId(1L);
        
        // When
        String toString = dto.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("id=1"));
    }
}