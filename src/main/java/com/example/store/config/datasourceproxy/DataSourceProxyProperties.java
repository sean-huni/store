package com.example.store.config.datasourceproxy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.TimeUnit;

/**
 * Configuration properties for DataSource Proxy slow query logging.
 * <p>
 * This class externalizes the threshold value and TimeUnit for the
 * logSlowQueryBySlf4j configuration in DataSourceProxyConfig.
 */
@Data
@Component
@ConfigurationProperties(prefix = "decorator.datasource.datasource-proxy.slow-query")
@Validated
public class DataSourceProxyProperties {

    /**
     * Threshold value for slow query detection.
     * Must be a positive number.
     */
    @NotNull(message = "Slow query threshold cannot be null")
    @Positive(message = "Slow query threshold must be positive")
    private Long threshold = 250L;

    /**
     * Time unit for the threshold value.
     * Common values: MILLISECONDS, MICROSECONDS, NANOSECONDS, SECONDS.
     */
    @NotNull(message = "Time unit cannot be null")
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
}