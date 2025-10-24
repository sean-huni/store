package test.config;

import com.example.store.aop.performance.SqlPerfAspect;
import com.example.store.aop.performance.context.SqlPerformanceContext;
import com.example.store.aop.performance.context.SqlPerformanceContextHolder;
import com.example.store.config.sqltracking.SqlLoggingListener;
import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.JpaConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

/**
 * Test configuration for database integration tests.
 * <p>
 * This configuration creates a PostgreSQL container using TestContainers.
 * It's used in repository tests with the @Import annotation.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
@Slf4j
public class TestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine3.22"))
                .withDatabaseName("store_int_db")
                .withUsername("postgres")
                .withPassword("postgres")
                .withStartupTimeout(Duration.ofSeconds(30))  // Increased from 2s to prevent connection issues
                .withConnectTimeoutSeconds(30)
                .withReuse(true);  // Enable container reuse to reduce startup overhead
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    SqlPerformanceContextHolder sqlPerformanceContextHolder() {
        return new SqlPerformanceContextHolder();
    }

    @Bean
    com.example.store.config.sqltracking.SqlPerformanceTrackingProperties sqlPerformanceTrackingProperties() {
        // Create test properties with default values
        return new com.example.store.config.sqltracking.SqlPerformanceTrackingProperties();
    }

    @Bean
    SqlPerfAspect enhancedSqlPerformanceAspect(
            SqlPerformanceContextHolder contextHolder,
            MeterRegistry meterRegistry,
            com.example.store.config.sqltracking.SqlPerformanceTrackingProperties properties) {
        return new SqlPerfAspect(contextHolder, meterRegistry, properties);
    }

    @Bean
    HypersistenceOptimizer hypersistenceOptimizer(EntityManagerFactory entityManagerFactory) {
        return new HypersistenceOptimizer(new JpaConfig(entityManagerFactory));
    }

    @Bean
    SqlLoggingListener customQueryLoggingListener(final MeterRegistry meterRegistry) {
        return new SqlLoggingListener(meterRegistry);
    }

    @Bean
    BeanPostProcessor sqlPerformanceDataSourcePostProcessor(
            SqlPerformanceContextHolder contextHolder,
            SqlLoggingListener sqlLoggingListener) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {
                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .name("SQL-Performance-Tracker-Test")
                            .listener(sqlLoggingListener)
                            .listener(new QueryExecutionListener() {
                                @Override
                                public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
                                    // Reduce logging overhead - only log in debug mode
                                    if (log.isDebugEnabled()) {
                                        SqlPerformanceContext context = contextHolder.peek();
                                        String operationName = context != null ? context.getOperationName() : "Unknown";

                                        log.debug("SQL Execution Starting - Operation: {} | Connection: {} | Queries: {}",
                                                operationName, executionInfo.getConnectionId(), list.size());
                                    }
                                }

                                @Override
                                public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
                                    // Essential: Record query performance data
                                    SqlPerformanceContext context = contextHolder.peek();
                                    if (context != null) {
                                        for (final QueryInfo queryInfo : queryInfoList) {
                                            context.recordQuery(queryInfo.getQuery(), execInfo.getElapsedTime());
                                        }
                                    }
                                }
                            })
                            .build();
                }
                return bean;
            }
        };
    }
}
