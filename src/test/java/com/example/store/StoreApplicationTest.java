package com.example.store;

import com.example.store.integration.config.IntTestConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@Tag("integration")
@SpringBootTest
@ActiveProfiles("int")
@Testcontainers
@Import(IntTestConfig.class)
@DisplayName("StoreApplication Tests")
class StoreApplicationTest {

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        // This test will fail if the application context cannot be loaded
    }
    
    @Test
    @DisplayName("StoreApplication can be instantiated")
    void canCreateStoreApplicationInstance() {
        // This test ensures the constructor is covered
        StoreApplication application = new StoreApplication();
        assertNotNull(application);
    }
}