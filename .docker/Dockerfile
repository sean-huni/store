# =============================================================================
# STAGE 1: Build Custom JRE with jlink (reduces final image size by ~150MB)
# =============================================================================
FROM eclipse-temurin:25-jdk-alpine AS jre-builder

# Install dependencies for jlink
RUN apk add --no-cache binutils

# Create custom minimal JRE with only required modules for Spring Boot + ZGC
RUN jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,java.xml,jdk.httpserver,jdk.jfr,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /opt/jre

# =============================================================================
# STAGE 2: Application Dependencies Layer (for better caching)
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
# STAGE 3: Final Production Image (Minimal & Secure)
# =============================================================================
FROM alpine:3.22.2

# Metadata for cloud deployments
LABEL maintainer="store-app-team" \
      version="2.0.0" \
      description="Multi-stage cloud-native Spring Boot application with custom JRE" \
      java.version="25-custom" \
      gc.collector="ZGC" \
      optimization="jlink-multi-stage"

# Security: Add app user with proper UID for K8s security contexts
ARG APPLICATION_USER=appuser
ARG APPLICATION_UID=1000
ARG APPLICATION_GID=1000

# Build argument for Spring Profiles (fixes undefined variable warning)
ARG SPRING_PROFILES_ACTIVE=cloud

# Create user and group (no shell for security)
RUN addgroup -g ${APPLICATION_GID} -S ${APPLICATION_USER} && \
    adduser -u ${APPLICATION_UID} -S ${APPLICATION_USER} -G ${APPLICATION_USER} -s /sbin/nologin

# Copy minimal dependencies from deps stage
COPY --from=deps-builder /usr/bin/dumb-init /usr/bin/dumb-init
COPY --from=deps-builder /usr/bin/curl /usr/bin/curl
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

# Container memory optimizations (more aggressive for custom JRE)
ENV MALLOC_ARENA_MAX=1
ENV MALLOC_TRIM_THRESHOLD_=65536
ENV MALLOC_TOP_PAD_=65536

# Cloud deployment environment variables
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-cloud}
ENV SERVER_SHUTDOWN=graceful
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

# Create application directories with proper ownership
RUN mkdir -p /app /tmp/heapdumps /app/logs && \
    chown -R ${APPLICATION_UID}:${APPLICATION_GID} /app /tmp/heapdumps

# Copy application JAR with proper ownership (this layer changes most frequently)
COPY --chown=${APPLICATION_UID}:${APPLICATION_GID} build/libs/store.jar /app/store.jar

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
    "-XX:InitialRAMPercentage=40.0", \
    "-XX:MinRAMPercentage=25.0", \
    "-XX:ZCollectionInterval=2", \
    "-XX:ZUncommitDelay=180", \
    "-XX:+UseLargePages", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/heapdumps/", \
    "-XX:OnOutOfMemoryError=echo 'OOM in K8s pod at $(date)' >> /app/logs/oom.log", \
    "-XX:ActiveProcessorCount=0", \
    "-Djdk.virtualThreadScheduler.parallelism=16", \
    "-Djdk.virtualThreadScheduler.maxPoolSize=512", \
    "-Djdk.virtualThreadScheduler.minRunnable=4", \
    "-Dmanagement.endpoints.web.exposure.include=health,info,metrics,prometheus", \
    "-Dmanagement.endpoint.health.show-details=when-authorized", \
    "-Dserver.shutdown=graceful", \
    "-Dspring.lifecycle.timeout-per-shutdown-phase=30s", \
    "-Dspring.jmx.enabled=false", \
    "-Dspring.main.lazy-initialization=false", \
    "-Djava.security.policy=all.policy", \
    "-jar", "/app/store.jar"]