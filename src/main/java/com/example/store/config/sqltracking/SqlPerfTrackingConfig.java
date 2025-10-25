package com.example.store.config.sqltracking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(
        prefix = "sql-performance.tracking",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Configuration
public class SqlPerfTrackingConfig {
    // Configuration beans...
}