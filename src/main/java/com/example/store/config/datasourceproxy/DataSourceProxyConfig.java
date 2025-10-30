package com.example.store.config.datasourceproxy;

import com.example.store.config.sqltracking.SqlLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceProxyConfig {

    @Bean
    @ConditionalOnProperty(
            name = "decorator.datasource.datasource-proxy.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public BeanPostProcessor dataSourceProxyBeanPostProcessor(
            SqlLoggingListener sqlLoggingListener,
            DataSourceProxyProperties dataSourceProxyProperties) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {

                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .name("Slow-SQL-Datasource-Proxy")
                            .listener(sqlLoggingListener)
                            .multiline()
                            .countQuery()
                            .logSlowQueryBySlf4j(dataSourceProxyProperties.getThreshold(), dataSourceProxyProperties.getTimeUnit())
                            .build();
                }
                return bean;
            }
        };
    }
}