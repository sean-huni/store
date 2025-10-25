package com.example.store.config.hypersistence;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.Config;
import io.hypersistence.optimizer.core.config.JpaConfig;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class HypersistenceConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "hypersistence.optimizer",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public HypersistenceOptimizer hypersistenceOptimizer(final EntityManagerFactory entityManagerFactory) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Config.Property.Session.TIMEOUT_MILLIS, 1000);
        properties.put(Config.Property.Session.FLUSH_TIMEOUT_MILLIS, 500);
        log.info("✨ Initializing Hypersistence Optimizer...");

        return new HypersistenceOptimizer(new JpaConfig(entityManagerFactory).setProperties(properties));
    }
//

    // Removed problematic SessionFactory beans that were conflicting with Spring Boot's auto-configuration
    // The HypersistenceOptimizer works directly with the auto-configured EntityManagerFactory
    // These custom SessionFactory beans were causing bean resolution conflicts

    @EventListener(ApplicationReadyEvent.class)
    public void logOptimizationEvents() {
        log.info("✨ " + "=".repeat(80));
        log.info("✨ Hypersistence Optimizer - Startup Validation Results");
        log.info("✨ " + "=".repeat(80));

        // Collect and report these
        // The optimizer will log warnings automatically during startup

        /*

            ### Example Output When Starting Application
            ```
            2025-10-23 10:15:32.456  INFO --- Initializing hypersistence Optimizer...

            2025-10-23 10:15:32.789  WARN --- [hypersistence Optimizer]
            ╔═══════════════════════════════════════════════════════════════════════════════
            ║ EAGER FETCHING DETECTED
            ╠═══════════════════════════════════════════════════════════════════════════════
            ║ Entity: com.example.demo.entity.BadOrder
            ║ Association: items
            ║ Type: OneToMany
            ║ Issue: Using FetchType.EAGER can cause performance problems
            ║
            ║ Recommendation:
            ║ - Change to FetchType.LAZY
            ║ - Use entity graphs or fetch joins when you need eager loading
            ║ - Selective eager loading is better than global eager loading
            ╚═══════════════════════════════════════════════════════════════════════════════

            2025-10-23 10:15:32.821  WARN --- [hypersistence Optimizer]
            ╔═══════════════════════════════════════════════════════════════════════════════
            ║ BIDIRECTIONAL ASSOCIATION WITHOUT HELPER METHODS
            ╠═══════════════════════════════════════════════════════════════════════════════
            ║ Entity: com.example.demo.entity.Customer
            ║ Association: orders
            ║ Type: OneToMany
            ║ Issue: Bidirectional association without proper helper methods
            ║
            ║ Recommendation:
            ║ - Add helper methods to keep both sides synchronized:
            ║   public void addOrder(Order order) {
            ║       orders.add(order);
            ║       order.setCustomer(this);
            ║   }
            ╚═══════════════════════════════════════════════════════════════════════════════

            2025-10-23 10:15:32.856  INFO --- Application started successfully
         */
    }
}
