package com.example.store.aop.performance.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

// Enhanced annotation with more features
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TrackSqlPerf {

    /**
     * Operation name for logging (defaults to method name)
     */
    String value() default "";

    /**
     * Warning threshold value
     */
    long warnThreshold() default 500;

    /**
     * Error threshold value
     */
    long errorThreshold() default 1000;

    /**
     * Time unit for thresholds (NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS)
     * Default: MILLISECONDS for backward compatibility
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * Maximum expected query count
     */
    int maxExpectedQueries() default 10;

    /**
     * Whether to fail fast if threshold exceeded
     */
    boolean failFast() default false;

    /**
     * Whether to track query count
     */
    boolean trackQueryCount() default true;

    /**
     * Whether to log individual queries
     */
    boolean logQueries() default false;

    /**
     * Custom metric tags
     */
    String[] metricTags() default {};

    /**
     * Whether this operation is critical and should be monitored even in production
     * When critical-operations-only mode is enabled, only operations marked as critical=true will be tracked
     */
    boolean critical() default false;
}
