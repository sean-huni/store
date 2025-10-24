package com.example.store.config.sqltracking;

import com.example.store.aop.performance.context.SqlPerformanceContext;
import com.example.store.aop.performance.context.SqlPerformanceContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class SqlPerfDSConfig { // Sql-Performance-DataSource-Config

    @Bean
    public static BeanPostProcessor sqlPerfDSPostProcessor(
            SqlPerformanceContextHolder contextHolder,
            SqlLoggingListener sqlLoggingListener) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {

                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .name("SQL-Performance-Tracker")
                            .listener(sqlLoggingListener)
                            .listener(new QueryExecutionListener() {
                                @Override
                                public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
                                    SqlPerformanceContext context = contextHolder.peek();
                                    String operationName = context != null ? context.getOperationName() : "Unknown";

                                    if (log.isDebugEnabled()) {
                                        log.debug("SQL Execution Starting - Operation: {} | Connection: {} | Batch: {} | Queries: {}",
                                                operationName,
                                                executionInfo.getConnectionId(),
                                                executionInfo.isBatch(),
                                                list.size());

                                        // Log individual queries for debugging
                                        list.forEach(queryInfo ->
                                                log.debug("  Query: {}", queryInfo.getQuery().replaceAll("\\s+", " ").trim())
                                        );
                                    } else {
                                        log.info("Test SQL Starting - Operation: {} | Queries: {}", operationName, list.size());
                                    }
                                }

                                @Override
                                public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
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