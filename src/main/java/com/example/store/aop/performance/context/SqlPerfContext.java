package com.example.store.aop.performance.context;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

// Performance tracking context
@Getter
public class SqlPerfContext {
    private final String operationName;
    private final long startTimeNanos;
    private final List<QueryExecution> queries = new ArrayList<>();
    private int queryCount = 0;

    public SqlPerfContext(String operationName) {
        this.operationName = operationName;
        this.startTimeNanos = System.nanoTime();
    }

    public void recordQuery(String sql, long executionTimeNanos) {
        queries.add(new QueryExecution(sql, executionTimeNanos));
        queryCount++;
    }

    public long getTotalExecutionTimeNanos() {
        return System.nanoTime() - startTimeNanos;
    }

    public long getTotalQueryTime() {
        return queries.stream()
                .mapToLong(QueryExecution::executionTimeNanos)
                .sum();
    }

    public record QueryExecution(String sql, long executionTimeNanos) {
    }
}