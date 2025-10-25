# =============================================================================
# STAGE 1: Build Custom JRE with jlink (reduces final image size by ~150MB)
# =============================================================================
FROM eclipse-temurin:25-jdk-alpine AS jre-builder

# Install dependencies for jlink
RUN apk add --no-cache binutils

# Create custom minimal JRE with only required modules for Spring Boot + ZGC
# FIXED: Added crypto modules for HTTPS/TLS (critical for fintech)
RUN jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.xml,java.xml.crypto,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.httpserver,jdk.jfr,jdk.management,jdk.management.agent,jdk.naming.dns,jdk.unsupported,jdk.zipfs \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /opt/jre

# =============================================================================
# STAGE 2: Extract Spring Boot Layers (CRITICAL for caching & faster deploys)
# =============================================================================
FROM eclipse-temurin:25-jdk-alpine AS extractor

WORKDIR /extract
COPY build/libs/*.jar app.jar

# Spring Boot's layertools extracts JAR into optimized layers
# This dramatically improves Docker layer caching
RUN java -Djarmode=layertools -jar app.jar extract

# =============================================================================
# STAGE 3: Application Dependencies Layer (for better caching)
# =============================================================================
FROM alpine:3.22.2 AS deps-builder

# Install minimal runtime dependencies
RUN apk add --no-cache \
        ca-certificates \
        tzdata \
        dumb-init \
        curl && \
    # Clean up APK cache \
    rm -rf /var/cache/apk/* && \
    # Setup timezone for UTC (cloud standard) \
    ln -snf /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone

# =============================================================================
# STAGE 4: Final Production Image (Minimal & Secure)
# =============================================================================
FROM alpine:3.22.2

# Metadata for cloud deployments
LABEL maintainer="store-app-team" \
      version="2.0.0" \
      description="Multi-stage cloud-native Spring Boot application with custom JRE" \
      java.version="25-Termurin" \
      gc.collector="ZGC" \
      optimization="jlink-multi-stage" \
      spring.boot.version="3.5.7"

# Security: Add app user with proper UID for K8s security contexts
ARG APPLICATION_USER=appuser
ARG APPLICATION_UID=1000
ARG APPLICATION_GID=1000

# Build argument for Spring Profiles
ARG SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# Create user and group (no shell for security)
RUN addgroup -g ${APPLICATION_GID} -S ${APPLICATION_USER} && \
    adduser -u ${APPLICATION_UID} -S ${APPLICATION_USER} -G ${APPLICATION_USER} -s /sbin/nologin

# Copy minimal dependencies from deps stage
COPY --from=deps-builder /usr/bin/dumb-init /usr/bin/dumb-init
COPY --from=deps-builder /usr/bin/curl /usr/bin/curl
COPY --from=deps-builder /lib/ld-musl-*.so.1 /lib/
COPY --from=deps-builder /lib/libc.musl-*.so.1 /lib/
COPY --from=deps-builder /usr/lib/libcurl.so.4 /usr/lib/
COPY --from=deps-builder /usr/lib/libnghttp2.so.14 /usr/lib/
COPY --from=deps-builder /usr/lib/libidn2.so.0 /usr/lib/
COPY --from=deps-builder /usr/lib/libssl.so.3 /usr/lib/
COPY --from=deps-builder /usr/lib/libcrypto.so.3 /usr/lib/
COPY --from=deps-builder /usr/lib/libunistring.so.5 /usr/lib/
COPY --from=deps-builder /usr/lib/libz.so.1 /usr/lib/
COPY --from=deps-builder /usr/lib/libbrotlidec.so.1 /usr/lib/
COPY --from=deps-builder /usr/lib/libbrotlicommon.so.1 /usr/lib/
COPY --from=deps-builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/
COPY --from=deps-builder /usr/share/zoneinfo /usr/share/zoneinfo
COPY --from=deps-builder /etc/localtime /etc/localtime
COPY --from=deps-builder /etc/timezone /etc/timezone

# Copy custom JRE from builder stage
COPY --from=jre-builder /opt/jre /opt/jre

# Set JAVA environment variables for custom JRE
ENV JAVA_HOME=/opt/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Cloud-aware JVM system properties (optimized for containers)
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom -Dspring.backgroundpreinitializer.ignore=true -Djava.awt.headless=true"

# Container memory optimizations
ENV MALLOC_ARENA_MAX=2
ENV MALLOC_TRIM_THRESHOLD_=131072
ENV MALLOC_MMAP_THRESHOLD_=131072

# Cloud deployment environment variables
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
ENV SERVER_SHUTDOWN=graceful
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

# Create application directories with proper ownership
RUN mkdir -p /app /tmp/heapdumps /app/logs && \
    chown -R ${APPLICATION_UID}:${APPLICATION_GID} /app /tmp/heapdumps /app/logs

# CRITICAL: Copy Spring Boot layers in dependency order
# Changes to app code won't invalidate dependency/snapshot layers
COPY --from=extractor --chown=${APPLICATION_UID}:${APPLICATION_GID} /extract/dependencies/ /app/
COPY --from=extractor --chown=${APPLICATION_UID}:${APPLICATION_GID} /extract/spring-boot-loader/ /app/
COPY --from=extractor --chown=${APPLICATION_UID}:${APPLICATION_GID} /extract/snapshot-dependencies/ /app/
COPY --from=extractor --chown=${APPLICATION_UID}:${APPLICATION_GID} /extract/application/ /app/

# Switch to non-root user for security
USER ${APPLICATION_UID}:${APPLICATION_GID}
WORKDIR /app

# Verify custom JRE is working (fail fast if jlink build was broken)
RUN java --version && java -XX:+UseZGC -version

# Lightweight health check (no bash dependency)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose application port for K8s service discovery
EXPOSE 8080

# Optimized ENTRYPOINT with custom JRE and enhanced ZGC configuration
# FIXED: Using JarLauncher instead of direct -jar for layered JARs
# IMPROVED: Adjusted ZGC settings for financial workloads
# Java 25 Preview Features + Container Integration + ZGC + Virtual Threads + Monitoring
# Configuration includes:
# - Java 25 Preview Features: --enable-preview
# - Container Integration: UseContainerSupport, PreferContainerQuotaForCPUCount  
# - ZGC Configuration: UseZGC, UnlockExperimentalVMOptions
# - Dynamic Memory Management: MaxRAMPercentage, InitialRAMPercentage, MinRAMPercentage
# - ZGC Tuning: ZCollectionInterval, ZUncommitDelay, UseLargePages
# - Enhanced Error Handling: ExitOnOutOfMemoryError, HeapDumpOnOutOfMemoryError
# - Virtual Thread Optimization: ActiveProcessorCount, virtualThreadScheduler settings
# - Monitoring & Observability: management endpoints exposure
# - Production Optimizations: graceful shutdown, lifecycle timeouts
# - Security: Java security policy
ENTRYPOINT ["dumb-init", "--", "java", \
    "--enable-preview", \
    "-XX:+UseContainerSupport", \
    "-XX:+PreferContainerQuotaForCPUCount", \
    "-XX:+UseZGC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:MinRAMPercentage=30.0", \
    "-XX:ZCollectionInterval=5", \
    "-XX:ZUncommitDelay=300", \
    "-XX:+AlwaysPreTouch", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdumps/heap-dump.hprof", \
    "-XX:ErrorFile=/app/logs/hs_err_pid%p.log", \
    "-XX:OnOutOfMemoryError=echo 'OOM in K8s pod at $(date)' >> /app/logs/oom.log", \
    "-XX:ActiveProcessorCount=0", \
    "-Djdk.virtualThreadScheduler.parallelism=0", \
    "-Djdk.virtualThreadScheduler.maxPoolSize=256", \
    "-Djdk.virtualThreadScheduler.minRunnable=2", \
    "-Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus", \
    "-Dmanagement.endpoint.health.show-details=when-authorized", \
    "-Dmanagement.metrics.export.prometheus.enabled=true", \
    "-Dserver.shutdown=graceful", \
    "-Dspring.lifecycle.timeout-per-shutdown-phase=30s", \
    "-Dspring.jmx.enabled=false", \
    "-Dspring.main.lazy-initialization=false", \
    "-Djava.security.policy=all.policy", \
    "org.springframework.boot.loader.launch.JarLauncher"]