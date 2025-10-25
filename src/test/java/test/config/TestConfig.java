package test.config;

import com.example.store.aop.performance.SqlPerfAspect;
import com.example.store.aop.performance.context.SqlPerfContext;
import com.example.store.aop.performance.context.SqlPerfContextHolder;
import com.example.store.config.sqltracking.SqlLoggingListener;
import com.example.store.config.sqltracking.SqlPerfTrackingProperties;
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
import org.springframework.context.annotation.Profile;
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
    SqlPerfContextHolder sqlPerformanceContextHolder() {
        return new SqlPerfContextHolder();
    }

    @Bean
    SqlPerfTrackingProperties sqlPerformanceTrackingProperties() {
        // Create test properties with default values
        return new SqlPerfTrackingProperties();
    }

    @Bean
    SqlPerfAspect enhancedSqlPerformanceAspect(
            SqlPerfContextHolder contextHolder,
            MeterRegistry meterRegistry,
            SqlPerfTrackingProperties properties) {
        return new SqlPerfAspect(contextHolder, meterRegistry, properties);
    }

    @Bean
    HypersistenceOptimizer hypersistenceOptimizer(EntityManagerFactory entityManagerFactory) {
        return new HypersistenceOptimizer(new JpaConfig(entityManagerFactory));
    }

    @Bean
    @Profile("test")
    SqlLoggingListener customQueryLoggingListener(final MeterRegistry meterRegistry, final SqlPerfContextHolder contextHolder) {
        return new SqlLoggingListener(meterRegistry, contextHolder);
    }

    @Bean
    @Profile("test")
    BeanPostProcessor sqlPerformanceDataSourcePostProcessor(
            final SqlPerfContextHolder contextHolder,
            final SqlLoggingListener sqlLoggingListener) {
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
                                public void beforeQuery(final ExecutionInfo executionInfo, final List<QueryInfo> list) {
                                    // Reduce logging overhead - only log in debug mode
                                    if (log.isDebugEnabled()) {
                                        SqlPerfContext context = contextHolder.peek();
                                        String operationName = context != null ? context.getOperationName() : "Unknown";
//
                                        log.debug(" SQL Execution Starting - Operation: {} | Connection: {} | Queries: {}",
                                                operationName, executionInfo.getConnectionId(), list.size());
                                    }
                                }

                                @Override
                                public void afterQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
                                    // Essential: Record query performance data
                                    SqlPerfContext context = contextHolder.peek();
                                    if (context != null) {
                                        for (final QueryInfo queryInfo : queryInfoList) {
                                            // Convert milliseconds to nanoseconds - execInfo.getElapsedTime() returns ms
                                            long executionTimeNanos = java.util.concurrent.TimeUnit.NANOSECONDS.convert(
                                                    execInfo.getElapsedTime(), java.util.concurrent.TimeUnit.MILLISECONDS);
                                            context.recordQuery(queryInfo.getQuery(), executionTimeNanos);
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
