package com.example.store.aop.performance;

import com.example.store.aop.performance.annotation.TrackSqlPerf;
import com.example.store.aop.performance.context.SqlPerfContext;
import com.example.store.aop.performance.context.SqlPerfContextHolder;
import com.example.store.config.sqltracking.SqlPerfTrackingProperties;
import com.example.store.exception.SqlPerformanceException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// Enhanced aspect with detailed tracking
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class SqlPerfAspect {
    private final SqlPerfContextHolder contextHolder;
    private final MeterRegistry meterRegistry;
    private final SqlPerfTrackingProperties properties;

    @Around("@annotation(trackSqlPerf)")
    public Object trackSqlExecution(final ProceedingJoinPoint joinPoint, final TrackSqlPerf trackSqlPerf) throws Throwable {

        // Check if we should skip this operation in critical-operations-only mode
        if (properties.criticalOperationsOnly() && !trackSqlPerf.critical()) {
            // Skip monitoring for non-critical operations in production
            return joinPoint.proceed();
        }

        final String operationName = determineOperationName(joinPoint, trackSqlPerf);
        final SqlPerfContext context = new SqlPerfContext(operationName);

        contextHolder.push(context);

        // Use high-precision timing
        long startNanos = System.nanoTime();

        try {
            Object result = joinPoint.proceed();

            long durationNanos = System.nanoTime() - startNanos;

            analyzePerformance(context, trackSqlPerf, durationNanos, joinPoint);

            return result;

        } catch (Throwable ex) {
            long durationNanos = System.nanoTime() - startNanos;
            logFailure(operationName, durationNanos, trackSqlPerf.timeUnit(), ex);
            throw ex;
        } finally {
            contextHolder.pop();
            if (contextHolder.peek() == null) {
                contextHolder.clear();
            }
        }
    }

    private void analyzePerformance(final SqlPerfContext context, final TrackSqlPerf config, final long durationNanos,
                                    final ProceedingJoinPoint joinPoint) {

        // Convert nanoseconds to configured unit
        final TimeUnit targetUnit = config.timeUnit();
        final long duration = targetUnit.convert(durationNanos, TimeUnit.NANOSECONDS);

        final int queryCount = context.getQueryCount();
        final long queryTimeNanos = context.getTotalQueryTime();
        final long queryTime = targetUnit.convert(queryTimeNanos, TimeUnit.NANOSECONDS);

        // Build performance report
        final var report = PerformanceReport.builder()
                .operationName(context.getOperationName())
                .totalExecutionTime(duration)
                .totalExecutionTimeNanos(durationNanos)
                .queryExecutionTime(queryTime)
                .queryExecutionTimeNanos(queryTimeNanos)
                .queryCount(queryCount)
                .queries(context.getQueries())
                .methodArgs(sanitizeArgs(joinPoint.getArgs()))
                .timeUnit(targetUnit)
                .build();

        // Apply environment-specific threshold multipliers
        long adjustedWarnThreshold = Math.round(config.warnThreshold() * properties.warnThresholdMultiplier());
        long adjustedErrorThreshold = Math.round(config.errorThreshold() * properties.errorThresholdMultiplier());

        // In strict mode, make thresholds even more restrictive
        if (properties.strictMode()) {
            adjustedWarnThreshold = Math.round(adjustedWarnThreshold * 0.8);
            adjustedErrorThreshold = Math.round(adjustedErrorThreshold * 0.8);
        }

        // Check thresholds with environment adjustments
        boolean hasWarning = duration >= adjustedWarnThreshold;
        boolean hasError = duration >= adjustedErrorThreshold;
        boolean hasExcessQueries = queryCount > config.maxExpectedQueries();

        // Always print detailed formatReport for all @TrackSqlPerf annotated methods
        if (hasError || hasExcessQueries) {
            log.error("⚠️ CRITICAL SQL Performance Issue:\n{}", formatReport(report, config));

            if (config.failFast()) {
                throw new SqlPerformanceException(
                        String.format("SQL performance threshold exceeded: %d%s > %d%s",
                                duration, getUnitSymbol(targetUnit),
                                config.errorThreshold(), getUnitSymbol(targetUnit))
                );
            }
        } else if (hasWarning) {
            log.warn("⚠️ SQL Performance Warning:\n{}", formatReport(report, config));
        } else {
            log.info("✓ SQL Performance OK: {} - {}{}, {} queries",
                    context.getOperationName(),
                    duration,
                    getUnitSymbol(targetUnit),
                    queryCount);
        }

        // Log detailed timing for critical operations
        if (config.timeUnit() == TimeUnit.MICROSECONDS ||
                config.timeUnit() == TimeUnit.NANOSECONDS) {
            log.debug("⚡ Precise timing for {}: {}ns ({}μs, {}ms)",
                    context.getOperationName(),
                    durationNanos,
                    TimeUnit.MICROSECONDS.convert(durationNanos, TimeUnit.NANOSECONDS),
                    TimeUnit.MILLISECONDS.convert(durationNanos, TimeUnit.NANOSECONDS)
            );
        }

        // Record metrics with appropriate precision
        recordMetrics(report, config);
    }

    private String formatReport(final PerformanceReport report, final TrackSqlPerf config) {
        String unitSymbol = getUnitSymbol(config.timeUnit());

        return String.format("""
                        ┌─────────────────────────────────────────────────────────────
                        │ Operation: %s
                        │ Total Time: %d%s (warn: %d%s, error: %d%s)
                        │ Query Time: %d%s
                        │ Query Count: %d (max: %d)
                        │ Overhead: %d%s (non-SQL time)
                        │ 
                        │ Precise Timing:
                        │   - Nanoseconds   : %,dns
                        │   - Microseconds  : %,dμs
                        │   - Milliseconds  : %,dms
                        │ 
                        │ Method Args: %s
                        %s
                        └─────────────────────────────────────────────────────────────
                        """,
                report.operationName(),
                report.totalExecutionTime(), unitSymbol,
                config.warnThreshold(), unitSymbol,
                config.errorThreshold(), unitSymbol,
                report.queryExecutionTime(), unitSymbol,
                report.queryCount(),
                config.maxExpectedQueries(),
                (report.totalExecutionTime() - report.queryExecutionTime()), unitSymbol,
                // Precise timing
                report.totalExecutionTimeNanos(),
                TimeUnit.MICROSECONDS.convert(report.totalExecutionTimeNanos(), TimeUnit.NANOSECONDS),
                TimeUnit.MILLISECONDS.convert(report.totalExecutionTimeNanos(), TimeUnit.NANOSECONDS),
                Arrays.toString(report.methodArgs()),
                formatSlowestQueries(report.queries(), config.timeUnit())
        );
    }

    private String formatSlowestQueries(final List<SqlPerfContext.QueryExecution> queries, final TimeUnit targetUnit) {
        if (queries.isEmpty()) {
            return "│ No queries executed";
        }

        String unitSymbol = getUnitSymbol(targetUnit);

        return "│ Slowest Queries:\n%s".formatted(
                queries.stream()
                        .sorted(Comparator.comparingLong(SqlPerfContext.QueryExecution::executionTimeNanos).reversed())
                        .limit(3)
                        .map(q -> {
                            long time = targetUnit.convert(q.executionTimeNanos(), TimeUnit.NANOSECONDS);
                            return String.format("│   - %d%s: %s",
                                    time,
                                    unitSymbol,
                                    truncate(q.sql(), 120));
                        })
                        .collect(Collectors.joining("\n")));
    }

    private void recordMetrics(final PerformanceReport report, final TrackSqlPerf config) {
        Tags tags = Tags.of(
                "operation", report.operationName(),
                "status", determineStatus(report, config),
                "time_unit", config.timeUnit().name().toLowerCase()
        );

        // Add custom tags
        for (final String tag : config.metricTags()) {
            final String[] parts = tag.split("=");
            if (parts.length == 2) {
                tags = tags.and(parts[0], parts[1]);
            }
        }

        // Record with nanosecond precision for Micrometer
        meterRegistry.timer("sql.performance.total", tags)
                .record(report.totalExecutionTimeNanos(), TimeUnit.NANOSECONDS);

        meterRegistry.timer("sql.performance.queries", tags)
                .record(report.queryExecutionTimeNanos(), TimeUnit.NANOSECONDS);

        meterRegistry.counter("sql.performance.query.count", tags)
                .increment(report.queryCount());

        // Track overhead (non-SQL time)
        long overheadNanos = report.totalExecutionTimeNanos() - report.queryExecutionTimeNanos();
        meterRegistry.timer("sql.performance.overhead", tags)
                .record(overheadNanos, TimeUnit.NANOSECONDS);
    }

    private String getUnitSymbol(final TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "ns";
            case MICROSECONDS -> "μs";
            case MILLISECONDS -> "ms";
            case SECONDS -> "s";
            default -> unit.name();
        };
    }

    private String determineStatus(final PerformanceReport report, final TrackSqlPerf config) {
        if (report.totalExecutionTime() >= config.errorThreshold() ||
                report.queryCount() > config.maxExpectedQueries()) {
            return "error";
        } else if (report.totalExecutionTime() >= config.warnThreshold()) {
            return "warning";
        }
        return "ok";
    }

    private void logFailure(final String operationName, final long durationNanos,
                            final TimeUnit configuredUnit, final Throwable ex) {
        long duration = configuredUnit.convert(durationNanos, TimeUnit.NANOSECONDS);
        log.error("SQL Performance tracking failed for {} after {}{}: {}",
                operationName,
                duration,
                getUnitSymbol(configuredUnit),
                ex.getMessage());
    }

    private String determineOperationName(final ProceedingJoinPoint joinPoint, final TrackSqlPerf annotation) {
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }

        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return "%s.%s".formatted(signature.getDeclaringType().getSimpleName(), signature.getName());
    }

    private Object[] sanitizeArgs(final Object[] args) {
        return Arrays.stream(args)
                .map(this::sanitizeArg)
                .toArray();
    }

    private String sanitizeArg(final Object arg) {
        final String NULL_LITERAL = "null";
        final String STRING_TYPE_LITERAL = "String";

        return switch (arg) {
            case null -> NULL_LITERAL;
            case String _ -> STRING_TYPE_LITERAL;
            case Number n -> n.toString();
            case LocalDate d -> d.toString();
            case LocalDateTime dt -> dt.toString();
            default -> arg.getClass().getSimpleName();
        };
    }

    private String truncate(final String str, final int maxLength) {
        return str.length() > maxLength
                ? "%s...".formatted(str.substring(0, maxLength))
                : str;
    }
}