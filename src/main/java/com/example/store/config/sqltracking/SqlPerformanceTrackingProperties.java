package com.example.store.config.sqltracking;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sql-performance.tracking")
@Validated
@Component
public record SqlPerformanceTrackingProperties(
        boolean enabled,
        boolean failFastOnError,
        boolean logQueries,
        boolean detailedLogging,
        boolean criticalOperationsOnly,
        boolean strictMode,
        double warnThresholdMultiplier,
        double errorThresholdMultiplier
) {
    public SqlPerformanceTrackingProperties() {
        this(true, false, true, true, false, false, 1.0, 1.0);
    }
}