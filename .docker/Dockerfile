# Dockerfile for Alpine containerization
FROM eclipse-temurin:24-jre-alpine

# Set the JAVA_HOME and PATH environment variables
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
# JVM system properties and basic settings
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Africa/Johannesburg -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
# Reduce native memory fragmentation in containers
ENV MALLOC_ARENA_MAX=2

# Add app user
ARG APPLICATION_USER=appuser
RUN adduser --no-create-home -u 1000 -D $APPLICATION_USER && \
    # Configure working directory
    mkdir /app && chown -R $APPLICATION_USER /app

USER 1000

# Copy the JAR file
COPY --chown=1000:1000 build/libs/store.jar /app/store.jar
WORKDIR /app

# Set timezone
ENV TZ=Africa/Johannesburg

# Switch to root user
USER root

# Install necessary packages
RUN apk update && \
    apk upgrade && \
    apk add --no-cache bash vim procps htop tzdata libc6-compat && \
    # Set the timezone \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone

USER 1000

# Expose the application port
EXPOSE 8080

# Set the entrypoint to run the application with Java 24 container-aware flags
ENTRYPOINT ["java", \
    "--enable-preview", \
    "-XX:+UseContainerSupport", \
    "-XX:+UseZGC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:MinRAMPercentage=25.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/tmp/", \
    "-XX:OnOutOfMemoryError=echo OOM occurred at $(date) >> /tmp/oom.log", \
    "-XX:+PreferContainerQuotaForCPUCount", \
    "-XX:ActiveProcessorCount=0", \
    "-Djdk.virtualThreadScheduler.parallelism=16", \
    "-Djdk.virtualThreadScheduler.maxPoolSize=256", \
    "-jar", "/app/store.jar"]