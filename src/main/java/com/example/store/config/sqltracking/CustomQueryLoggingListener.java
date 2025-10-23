package com.example.store.config.sqltracking;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CustomQueryLoggingListener implements QueryExecutionListener {

    private final ThreadLocal<List<QueryInfo>> queryStack =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // Track query start time with high precision
        queryStack.get().addAll(queryInfoList);
    }

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        long executionTime = execInfo.getElapsedTime();
        int queryCount = queryInfoList.size();

        for (QueryInfo queryInfo : queryInfoList) {
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
                log.warn("⚠️ POTENTIAL N+1 QUERY PATTERN: {}", truncate(query, 80));
            }
        }

        // Track metrics
        recordQueryMetrics(executionTime, queryCount, queryInfoList);

        queryStack.get().clear();
    }

    private boolean isSimilarToPreviousQuery(String currentQuery) {
        List<QueryInfo> queries = queryStack.get();
        if (queries.size() < 2) return false;

        // Simple heuristic: check if we're executing the same query pattern multiple times
        String normalized = normalizeQuery(currentQuery);
        long similarCount = queries.stream()
                .map(q -> normalizeQuery(q.getQuery()))
                .filter(normalized::equals)
                .count();

        return similarCount > 5;  // More than 5 similar queries = potential N+1
    }

    private String normalizeQuery(String query) {
        // Remove parameters and whitespace for comparison
        return query.replaceAll("\\?|\\d+|'[^']*'", "?")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private void recordQueryMetrics(long executionTime, int queryCount,
                                    List<QueryInfo> queryInfoList) {
        // Record to Micrometer or your metrics system
        // meterRegistry.timer("sql.query.time").record(executionTime, TimeUnit.MILLISECONDS);
        // meterRegistry.counter("sql.query.count").increment(queryCount);
    }

    private String formatQuery(String query) {
        // Basic formatting for readability
        return query.replaceAll("\\s+", " ")
                .replaceAll("(SELECT|FROM|WHERE|JOIN|LEFT|RIGHT|INNER|ON|AND|OR)", "\n$1")
                .trim();
    }

    private String truncate(String str, int maxLength) {
        return str.length() > maxLength
                ? str.substring(0, maxLength) + "..."
                : str;
    }
}