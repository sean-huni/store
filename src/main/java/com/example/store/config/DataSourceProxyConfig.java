package com.example.store.config;

import com.example.store.config.sqltracking.SqlLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

@Configuration
public class DataSourceProxyConfig {

    @Bean
    @ConditionalOnProperty(
            name = "decorator.datasource.datasource-proxy.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public BeanPostProcessor dataSourceProxyBeanPostProcessor(
            SqlLoggingListener customListener) {

        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (bean instanceof DataSource && !(bean instanceof ProxyDataSource)) {

                    return ProxyDataSourceBuilder
                            .create((DataSource) bean)
                            .name("SQL-Performance-Proxy")
                            .listener(customListener)
                            .multiline()
                            .countQuery()
                            .logSlowQueryBySlf4j(300, TimeUnit.MILLISECONDS)
                            .build();
                }
                return bean;
            }
        };
    }
}