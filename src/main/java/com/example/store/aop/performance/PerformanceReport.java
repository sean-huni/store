package com.example.store.aop.performance;

import com.example.store.aop.performance.context.SqlPerformanceContext;
import lombok.Builder;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Builder
record PerformanceReport(
        String operationName,
        long totalExecutionTime,
        long totalExecutionTimeNanos,
        long queryExecutionTime,
        long queryExecutionTimeNanos,
        int queryCount,
        List<SqlPerformanceContext.QueryExecution> queries,
        Object[] methodArgs,
        TimeUnit timeUnit
) {
}
