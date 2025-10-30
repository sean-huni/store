package com.example.store.aop.performance.dto;

import com.example.store.aop.performance.context.SqlPerfContext;
import lombok.Builder;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Builder
public record PerfReport(
        String operationName,
        long totalExecutionTime,
        long totalExecutionTimeNanos,
        long queryExecutionTime,
        long queryExecutionTimeNanos,
        int queryCount,
        List<SqlPerfContext.QueryExecution> queries,
        Object[] methodArgs,
        TimeUnit timeUnit
) {
}
