package test.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Test configuration for database integration tests.
 * <p>
 * This configuration creates a PostgreSQL container using TestContainers.
 * It's used in repository tests with the @Import annotation.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine3.22"))
                .withDatabaseName("store_int_db")
                .withUsername("postgres")
                .withPassword("postgres")
                .withStartupTimeout(Duration.ofSeconds(2));
    }
}
