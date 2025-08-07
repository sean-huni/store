package com.example.store.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;

@TestConfiguration(proxyBeanMethods = false)
public class IntTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                .withDatabaseName("store_int_db")
                .withUsername("postgres_int")
                .withPassword("postgres_int")
                .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
                .withStartupTimeout(Duration.ofSeconds(60));
    }
    
    @DynamicPropertySource
    static void additionalProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.liquibase.drop-first", () -> "true");
    }
}