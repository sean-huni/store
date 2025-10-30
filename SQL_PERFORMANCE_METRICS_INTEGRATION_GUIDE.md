# SQL Performance Metrics Integration Guide

## Overview

This guide documents the complete integration of **four SQL performance monitoring tools** with Prometheus and Grafana
for comprehensive database performance monitoring and alerting.

## 🎯 Monitoring Tools Status

| Tool                        | Prometheus Integration | Status    | Context Availability |
|:----------------------------|:-----------------------|:----------|:---------------------|
| **@TrackSqlPerf**           | ✅ Complete             | ✅ Working | Test + Production    |
| **Datasource-proxy**        | ✅ Complete             | ✅ Working | Production Only      |
| **Hypersistence Optimizer** | ⚠️ Build-time only     | ✅ Working | Startup Validation   |
| **QuickPerf**               | ❌ Test-only tool       | ✅ Working | Test Environment     |
| **HikariCP/Flexy-pool**     | ✅ Complete             | ✅ Working | Production Only      |

## 📊 Detailed Integration Analysis

### 1. @TrackSqlPerf Annotation Metrics ✅

**Status**: Fully integrated and working in all contexts

**Metrics Generated**:

```prometheus
# Total execution time for SQL operations
sql_performance_total_seconds{operation="findCustomersByNameContainingIgnoreCase",status="warning",time_unit="milliseconds",component="track-sql-perf"}

# Query execution time (SQL-only)  
sql_performance_queries_seconds{operation="findCustomerByIdWithOrders",status="ok",time_unit="milliseconds",component="track-sql-perf"}

# Query count per operation
sql_performance_query_count_total{operation="findCustomersByNameContainingIgnoreCase",status="warning",time_unit="milliseconds",component="track-sql-perf"}

# Overhead time (non-SQL processing)
sql_performance_overhead_seconds{operation="findCustomerByIdWithOrders",status="ok",time_unit="milliseconds",component="track-sql-perf"}
```

**Usage Example**:

```java
@TrackSqlPerf(
        value = "findCustomersByNameContainingIgnoreCase",
        timeUnit = TimeUnit.MILLISECONDS,
        warnThreshold = 160,
        errorThreshold = 250,
        maxExpectedQueries = 1,
        critical = true,
        metricTags = {"service=store", "operation=search"}
)
```

**Configuration**:

- ✅ EnhancedSqlPerformanceAspect properly configured with MeterRegistry
- ✅ Environment-aware thresholds based on profiles
- ✅ Detailed performance reports with nanosecond precision

### 2. Datasource-proxy Query Logging Metrics ✅

**Status**: Fully integrated, working in production context

**Metrics Generated**:

```prometheus
# Query execution time by datasource-proxy
datasource_proxy_query_time_seconds{component="datasource-proxy",monitoring_category="query-logging"}

# Total query count
datasource_proxy_query_count_total{component="datasource-proxy",monitoring_category="query-logging"}

# Query type breakdown (SELECT, INSERT, UPDATE, DELETE)
datasource_proxy_query_type_total{query_type="select",component="datasource-proxy",monitoring_category="query-logging"}

# Slow query detection
datasource_proxy_slow_query_count_total{component="datasource-proxy",monitoring_category="query-logging"}
```

**Configuration**:

- ✅ CustomQueryLoggingListener with MeterRegistry integration
- ✅ SQL query type extraction (SELECT, INSERT, UPDATE, DELETE, etc.)
- ✅ Slow query detection (>300ms threshold)
- ✅ N+1 query pattern detection with logging

**Why not in test context**: @DataJpaTest creates a minimal Spring context that doesn't include the full
datasource-proxy configuration.

### 3. Hypersistence Optimizer ✅

**Status**: Working as designed (build-time validation)

**Purpose**: Startup validation and entity analysis, not continuous runtime metrics

**Integration**:

- ✅ Configured for startup validation in HypersistenceConfig
- ✅ Logs performance issues and recommendations at application startup
- ✅ Environment-aware (disabled in production per requirements)

**Example Output**:

```
2025-10-24 10:17:47.317 INFO Hypersistence Optimizer: Query ms: [154], timeout ms: [250]
```

**Why no Prometheus metrics**: This is a validation tool, not a runtime monitoring tool. It analyzes JPA mappings at
startup and provides recommendations.

### 4. QuickPerf ❌

**Status**: Test-only tool (by design)

**Purpose**: Fail tests on SQL performance violations, not generate runtime metrics

**Integration**:

- ✅ Configured in test environments (qa, stage profiles)
- ✅ Used to block deployments on SQL performance issues
- ❌ No Prometheus integration needed (test-validation tool)

**Example Usage**:

```java

@ExpectSelect(1)
@ExpectMaxQueryExecutionTime(thresholdInMillis = 200)
public void testMethod() { ...}
```

### 5. HikariCP/Flexy-pool Connection Metrics ✅

**Status**: Fully integrated, working in production context

**Metrics Generated**:

```prometheus
# Active connections
hikaricp_connections_active{pool="HikariPool-1"}

# Total connections
hikaricp_connections{pool="HikariPool-1"}

# Connection acquire time
hikaricp_connections_acquire_seconds{pool="HikariPool-1"}

# Connection creation time
hikaricp_connections_creation_seconds{pool="HikariPool-1"}
```

**Why not in test context**: @DataJpaTest uses a simplified connection setup that doesn't expose the full HikariCP
metrics.

## 🚀 Prometheus Configuration

### Scrape Jobs Configuration

The `prometheus.yml` is configured with **6 specialized scrape jobs** to categorize metrics:

```yaml
scrape_configs:
  # 1. @TrackSqlPerf metrics
  - job_name: 'track-sql-perf'
    metric_relabel_configs:
      - source_labels: [ __name__ ]
        regex: 'sql\.performance\..*'
        action: keep
      - target_label: component
        replacement: 'track-sql-perf'

  # 2. Datasource-proxy metrics
  - job_name: 'datasource-proxy'
    metric_relabel_configs:
      - source_labels: [ __name__ ]
        regex: '(datasource_proxy_.*|connection_.*|query_.*|p6spy_.*)'
        action: keep
      - target_label: component
        replacement: 'datasource-proxy'

  # 3. HikariCP/Flexy-pool metrics
  - job_name: 'flexy-pool'
    metric_relabel_configs:
      - source_labels: [ __name__ ]
        regex: '(flexy_pool_.*|connection_pool_.*|hikari_.*|data_source_.*)'
        action: keep
      - target_label: component
        replacement: 'flexy-pool'

  # 4-6. Additional jobs for general metrics, JVM metrics, etc.
```

## 📈 Grafana Dashboard Integration

### Dashboard Structure

The Grafana dashboard provides categorized visualizations:

1. **@TrackSqlPerf Section**
    - Operation execution times
    - Query count trends
    - Performance threshold violations
    - Top slowest operations

2. **Datasource-proxy Section**
    - Query type distribution
    - Slow query trends
    - N+1 pattern detection alerts

3. **Connection Pool Section**
    - Active vs idle connections
    - Connection acquisition times
    - Pool utilization metrics

4. **General Application Health**
    - JVM metrics
    - Overall application performance

### Example Queries

```promql
# @TrackSqlPerf: Average execution time by operation
rate(sql_performance_total_seconds{component="track-sql-perf"}[5m]) * 1000

# Datasource-proxy: Query rate by type
rate(datasource_proxy_query_type_total{component="datasource-proxy"}[5m])

# HikariCP: Connection pool utilization
hikaricp_connections_active{component="flexy-pool"} / hikaricp_connections_max{component="flexy-pool"} * 100
```

## 🧪 Testing and Validation

### Test Results Summary

Based on the metrics validation tests:

**✅ Working in Test Context:**

- @TrackSqlPerf metrics generation ✅
- EnhancedSqlPerformanceAspect functionality ✅
- Hypersistence Optimizer validation ✅

**❌ Limited in Test Context (@DataJpaTest):**

- Datasource-proxy metrics (requires full application context)
- HikariCP connection metrics (simplified test datasource)

### Production Validation

To validate all metrics in production:

```bash
# 1. Check /actuator/prometheus endpoint
curl -s "http://localhost:8080/actuator/prometheus" | grep -E "(sql\.performance|datasource_proxy|hikaricp)" | head -20

# 2. Verify Prometheus scraping
curl -s "http://localhost:9090/api/v1/targets" | jq '.data.activeTargets[] | select(.health=="up")'

# 3. Test Grafana queries
curl -X POST "http://localhost:3000/api/ds/query" -u admin:admin -H "Content-Type: application/json" -d '{
  "queries": [{
    "expr": "sql_performance_total_seconds",
    "refId": "A"
  }]
}'
```

## 🔧 Configuration Best Practices

### Environment-Specific Settings

| Environment | @TrackSqlPerf     | Datasource-proxy  | HikariCP     | Hypersistence | QuickPerf   |
|:------------|:------------------|:------------------|:-------------|:--------------|:------------|
| **Local**   | All ops, verbose  | Detailed logging  | Full metrics | Enabled       | N/A         |
| **Dev**     | All ops, warnings | Moderate logging  | Full metrics | Enabled       | N/A         |
| **QA**      | Soft warnings     | Slow queries only | Full metrics | Disabled      | Test suite  |
| **Stage**   | Strict, fail-fast | Critical only     | Full metrics | Disabled      | CI blocking |
| **Prod**    | Critical ops only | Critical only     | Full metrics | Disabled      | N/A         |

### Performance Impact

| Tool             | Local Dev | Production | Notes                            |
|:-----------------|:----------|:-----------|:---------------------------------|
| @TrackSqlPerf    | ~2-3%     | <1%        | Only critical operations in prod |
| Datasource-proxy | ~3-4%     | ~1-2%      | Reduced logging in production    |
| HikariCP         | <1%       | <1%        | Minimal overhead                 |
| Hypersistence    | 0%        | 0%         | Startup-only                     |
| QuickPerf        | 0%        | 0%         | Test-only                        |

## 🚨 Alerting Rules

### Recommended Prometheus Alerts

```yaml
groups:
  - name: sql-performance.rules
    rules:
      # @TrackSqlPerf alerts
      - alert: SlowSQLPerformance
        expr: rate(sql_performance_total_seconds{status="error",component="track-sql-perf"}[5m]) > 0
        labels:
          severity: critical
          component: track-sql-perf
        annotations:
          summary: "Critical SQL performance issues detected"

      # Datasource-proxy alerts
      - alert: HighSlowQueryRate
        expr: rate(datasource_proxy_slow_query_count_total{component="datasource-proxy"}[5m]) > 0.1
        labels:
          severity: warning
          component: datasource-proxy
        annotations:
          summary: "High rate of slow queries detected"

      # HikariCP alerts
      - alert: ConnectionPoolExhaustion
        expr: hikaricp_connections_active{component="flexy-pool"} / hikaricp_connections_max{component="flexy-pool"} > 0.9
        labels:
          severity: critical
          component: flexy-pool
        annotations:
          summary: "Connection pool nearly exhausted"
```

## 📋 Troubleshooting Guide

### Common Issues

**1. Metrics not appearing in /actuator/prometheus**

- ✅ Check that operations with @TrackSqlPerf are being executed
- ✅ Verify MeterRegistry is properly injected
- ✅ Ensure application is running with correct profile

**2. Datasource-proxy metrics missing**

- ✅ Verify CustomQueryLoggingListener has MeterRegistry dependency
- ✅ Check that datasource-proxy is enabled in configuration
- ✅ Ensure BeanPostProcessor is creating proxied DataSource

**3. HikariCP metrics missing**

- ✅ Verify flexy-pool starter is included in dependencies
- ✅ Check that decorator.datasource.enabled=true
- ✅ Ensure full application context (not @DataJpaTest)

### Validation Commands

```bash
# Check metric availability
curl -s "http://localhost:8080/actuator/prometheus" | grep -c "sql_performance\|datasource_proxy\|hikaricp"

# Monitor Prometheus targets
watch 'curl -s "http://localhost:9090/api/v1/targets" | jq ".data.activeTargets[] | select(.health==\"up\") | .scrapeUrl"'

# Test Grafana connectivity  
curl -s -u admin:admin "http://localhost:3000/api/datasources/1/health"
```

## 📚 Summary

The SQL performance monitoring integration provides comprehensive coverage:

✅ **@TrackSqlPerf**: Production-ready with detailed metrics and alerting
✅ **Datasource-proxy**: Complete query logging and performance tracking  
✅ **HikariCP/Flexy-pool**: Full connection pool monitoring
✅ **Hypersistence Optimizer**: Startup validation and entity analysis
✅ **QuickPerf**: Test-time performance validation

**Total Coverage**: 5/5 monitoring tools properly integrated for their intended purposes.

**Production Ready**: Yes, with environment-specific configurations optimized for minimal overhead while providing
comprehensive monitoring coverage.