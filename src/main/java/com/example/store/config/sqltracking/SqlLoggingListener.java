package com.example.store.config.sqltracking;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class SqlLoggingListener implements QueryExecutionListener {
    private final ThreadLocal<List<QueryInfo>> queryStack = ThreadLocal.withInitial(ArrayList::new);
    private final MeterRegistry meterRegistry;

    @Override
    public void beforeQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        // Track query start time with high precision
        queryStack.get().addAll(queryInfoList);
    }

    @Override
    public void afterQuery(final ExecutionInfo execInfo, final List<QueryInfo> queryInfoList) {
        final long executionTime = execInfo.getElapsedTime();
        final int queryCount = queryInfoList.size();

        for (final QueryInfo queryInfo : queryInfoList) {
            String query = queryInfo.getQuery();

            // Log slow queries with details
            if (executionTime > 300) {  // 300ms threshold
                log.warn("""
                                ⚠️ SLOW QUERY DETECTED
                                Time: {}ms
                                Query: {}
                                Connection: {}
                                """,
                        executionTime,
                        formatQuery(query),
                        execInfo.getConnectionId()
                );
            } else if (log.isDebugEnabled()) {
                log.debug("Query executed in {}ms: {}",
                        executionTime,
                        truncate(query, 100));
            }

            // Detect potential N+1 patterns
            if (isSimilarToPreviousQuery(query)) {
                log.warn("⚠️ POTENTIAL N+1 QUERY PATTERN: {}", truncate(query, 100));
            }
        }

        // Track metrics
        recordQueryMetrics(executionTime, queryCount, queryInfoList);

        queryStack.get().clear();
    }

    private boolean isSimilarToPreviousQuery(final String currentQuery) {
        final List<QueryInfo> queries = queryStack.get();
        if (queries.size() < 2) return false;

        // Simple heuristic: check if we're executing the same query pattern multiple times
        final String normalized = normalizeQuery(currentQuery);
        final long similarCount = queries.stream()
                .map(q -> normalizeQuery(q.getQuery()))
                .filter(normalized::equals)
                .count();

        return similarCount > 5;  // More than 5 similar queries = potential N+1
    }

    private String normalizeQuery(final String query) {
        // Remove parameters and whitespace for comparison
        return query.replaceAll("\\?|\\d+|'[^']*'", "?")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void recordQueryMetrics(final long executionTime, final int queryCount,
                                    final List<QueryInfo> queryInfoList) {
        // Record datasource-proxy metrics to Prometheus via Micrometer
        if (meterRegistry != null) {
            // Record query execution time
            meterRegistry.timer("datasource_proxy_query_time_seconds",
                            "component", "datasource-proxy",
                            "monitoring_category", "query-logging")
                    .record(executionTime, TimeUnit.MILLISECONDS);

            // Record query count
            meterRegistry.counter("datasource_proxy_query_count_total",
                            "component", "datasource-proxy",
                            "monitoring_category", "query-logging")
                    .increment(queryCount);

            // Record individual query metrics
            for (final QueryInfo queryInfo : queryInfoList) {
                final String queryType = extractQueryType(queryInfo.getQuery());
                meterRegistry.counter("datasource_proxy_query_type_total",
                                "query_type", queryType,
                                "component", "datasource-proxy",
                                "monitoring_category", "query-logging")
                        .increment();
            }

            // Record slow query metrics
            if (executionTime > 300) {
                meterRegistry.counter("datasource_proxy_slow_query_count_total",
                                "component", "datasource-proxy",
                                "monitoring_category", "query-logging")
                        .increment();
            }
        }
    }

    private String formatQuery(final String query) {
        // Basic formatting for readability
        return query.replaceAll("\\s+", " ")
                .replaceAll("(SELECT|FROM|WHERE|JOIN|LEFT|RIGHT|INNER|ON|AND|OR)", "\n$1")
                .trim();
    }

    private String truncate(final String str, final int maxLength) {
        return str.length() > maxLength
                ? "%s...".formatted(str.substring(0, maxLength))
                : str;
    }

    /**
     * Optimized query type extraction using Java 25 features.
     * Uses enhanced switch expressions and pattern matching for improved performance and readability.
     *
     * @param query the SQL query string to analyze
     * @return the query type as a lowercase string
     */
    private static String extractQueryType(final String query) {
        // Early return for null/empty queries using modern null-safety patterns
        if (query == null || query.isBlank()) {
            return "unknown";
        }

        // Extract first SQL keyword efficiently using Java 25 string processing
        final String firstWord = query.trim()
                .toLowerCase()
                .split("\\s+", 2)[0]; // Limit split to 2 parts for performance

        // Java 25 enhanced switch expression with pattern matching and exhaustive coverage
        return switch (firstWord) {
            case "select" -> "select";
            case "insert" -> "insert";
            case "update" -> "update";
            case "delete" -> "delete";
            case "create" -> "create";
            case "drop" -> "drop";
            case "alter" -> "alter";
            case "truncate" -> "truncate";
            case "with" -> "select"; // CTE (Common Table Expression) queries are essentially SELECT
            case "merge" -> "merge"; // MERGE statements (UPSERT operations)
            case "call" -> "procedure"; // Stored procedure calls
            case "exec", "execute" -> "procedure"; // Alternative procedure execution syntax
            default -> {
                // Handle complex cases with additional analysis
                if (firstWord.startsWith("/*")) {
                    // Handle queries starting with comments - extract actual SQL command
                    final String withoutComments = query.replaceAll("/\\*.*?\\*/", "").trim();
                    yield withoutComments.isEmpty() ? "other" : extractQueryType(withoutComments);
                }
                yield "other";
            }
        };
    }
}