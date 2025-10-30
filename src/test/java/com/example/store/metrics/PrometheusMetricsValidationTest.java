package com.example.store.metrics;

import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.repo.CustomerRepo;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import test.config.TestConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to validate that SQL performance monitoring tools are properly
 * configured and generating Prometheus metrics.
 */
@Tag("metrics")
@ActiveProfiles("db")
@DataJpaTest
@Import({TestConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@Slf4j
@DisplayName("Prometheus Metrics Validation")
class PrometheusMetricsValidationTest {
    @Autowired
    private CustomerRepo customerRepo;
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("Should generate @TrackSqlPerf metrics for search operations")
    void shouldGenerateTrackSqlPerfMetricsForSearch() {
        // Given: Initial metric count
        double initialSearchMetrics = getMetricCount("sql.performance.total");

        // When: Execute a method with @TrackSqlPerf annotation
        final Pageable pageable = PageRequest.of(0, 5);
        final String searchTerm = "test";
        final List<Customer> customers = customerRepo.findCustomersByNameContainingIgnoreCase(searchTerm, pageable);

        // Then: Verify @TrackSqlPerf metrics were generated
        double finalSearchMetrics = getMetricCount("sql.performance.total");
        assertTrue(finalSearchMetrics > initialSearchMetrics, "Expected @TrackSqlPerf metrics to be generated for search operation");

        log.info("✅ @TrackSqlPerf metrics validated - Search metrics: {} -> {}", initialSearchMetrics, finalSearchMetrics);
    }

    @Test
    @DisplayName("Should generate @TrackSqlPerf metrics for fetch operations")
    void shouldGenerateTrackSqlPerfMetricsForFetch() {
        // Given: Initial metric count
        double initialFetchMetrics = getMetricCount("sql.performance.total");

        // When: Execute a method with @TrackSqlPerf annotation
        final Long customerId = 13L; // Customer with orders
        final Optional<Customer> customer = customerRepo.findCustomerByIdWithOrders(customerId);

        // Then: Verify @TrackSqlPerf metrics were generated
        double finalFetchMetrics = getMetricCount("sql.performance.total");
        assertTrue(finalFetchMetrics > initialFetchMetrics, "Expected @TrackSqlPerf metrics to be generated for fetch operation");

        assertTrue(customer.isPresent(), "Expected to find customer with ID %d".formatted(customerId));

        log.info("✅ @TrackSqlPerf metrics validated - Fetch metrics: {} -> {}", initialFetchMetrics, finalFetchMetrics);
    }

    @Test
    @DisplayName("Should generate datasource-proxy metrics (if available)")
    void shouldGenerateDatasourceProxyMetrics() {
        // Given: Check if datasource-proxy metrics are available in test environment
        boolean hasDataSourceProxyMetrics = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().contains("datasource_proxy"));

        if (!hasDataSourceProxyMetrics) {
            log.info("⚠️ Datasource-proxy metrics not available in test environment - skipping validation");
            return;
        }

        // Given: Initial metric counts
        double initialQueryCount = getMetricCount("datasource_proxy_query_count_total");
        double initialQueryTime = getTimerCount("datasource_proxy_query_time_seconds");

        // When: Execute database operations that will trigger datasource-proxy
        final Pageable pageable = PageRequest.of(0, 3);
        final List<Customer> customers = customerRepo.findCustomersByNameContainingIgnoreCase("a", pageable);

        // Then: Verify datasource-proxy metrics were generated
        double finalQueryCount = getMetricCount("datasource_proxy_query_count_total");
        double finalQueryTime = getTimerCount("datasource_proxy_query_time_seconds");

        assertTrue(finalQueryCount > initialQueryCount, "Expected datasource-proxy query count metrics to be generated");
        assertTrue(finalQueryTime > initialQueryTime, "Expected datasource-proxy query time metrics to be generated");

        log.info("✅ Datasource-proxy metrics validated - Query count: {} -> {}, Query time: {} -> {}", initialQueryCount, finalQueryCount, initialQueryTime, finalQueryTime);
    }

    @Test
    @DisplayName("Should generate HikariCP connection pool metrics (if available)")
    void shouldGenerateHikariCPMetrics() {
        // Given: Check if HikariCP metrics are available in test environment
        boolean hasHikariMetrics = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().contains("hikari"));

        if (!hasHikariMetrics) {
            log.info("⚠️ HikariCP metrics not available in test environment - skipping validation");
            return;
        }

        // Given: HikariCP metrics should be available
        double activeConnections = getGaugeValue("hikari_connections_active");
        double totalConnections = getGaugeValue("hikari_connections");

        // Then: Verify HikariCP metrics are present
        assertTrue(activeConnections >= 0, "Expected active connections metric to be present");
        assertTrue(totalConnections >= 0, "Expected total connections metric to be present");

        log.info("✅ HikariCP metrics validated - Active: {}, Total: {}", activeConnections, totalConnections);
    }

    @Test
    @DisplayName("Should validate all metric components are configured")
    void shouldValidateAllMetricComponents() {
        // Given: Execute operations to generate metrics
        customerRepo.findCustomersByNameContainingIgnoreCase("validation", PageRequest.of(0, 2));

        // Then: Verify each monitoring component has metrics (only if available)
        assertMetricExists("sql.performance.total", "@TrackSqlPerf");

        // Check datasource-proxy metrics conditionally
        boolean hasDataSourceProxyMetrics = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().contains("datasource_proxy"));
        if (hasDataSourceProxyMetrics) {
            assertMetricExists("datasource_proxy_query_count_total", "Datasource-proxy");
        } else {
            log.info("⚠️ Datasource-proxy metrics not available - skipping validation");
        }

        // Check HikariCP metrics conditionally
        boolean hasHikariMetrics = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().contains("hikari"));
        if (hasHikariMetrics) {
            assertMetricExists("hikari_connections_active", "HikariCP/Flexy-pool");
        } else {
            log.info("⚠️ HikariCP metrics not available - skipping validation");
        }

        log.info("✅ All available SQL monitoring components validated successfully");
    }

    private double getMetricCount(final String metricName) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(metricName))
                .mapToDouble(meter -> meter.measure().iterator().next().getValue())
                .sum();
    }

    private double getTimerCount(final String metricName) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(metricName))
                .filter(meter -> meter.getId().getTag("statistic") != null)
                .filter(meter -> "count".equals(meter.getId().getTag("statistic")))
                .mapToDouble(meter -> meter.measure().iterator().next().getValue())
                .sum();
    }

    private double getGaugeValue(String metricName) {
        return meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().equals(metricName))
                .mapToDouble(meter -> meter.measure().iterator().next().getValue())
                .findFirst()
                .orElse(-1.0);
    }

    private void assertMetricExists(String metricName, String component) {
        boolean exists = meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().equals(metricName));
        assertTrue(exists, String.format("Expected %s metric '%s' to exist", component, metricName));
    }
}