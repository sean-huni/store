# Dockerfile for Alpine containerization
FROM eclipse-temurin:24-jre-alpine

# Set the JAVA_HOME and PATH environment variables
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Add app user
ARG APPLICATION_USER=appuser
RUN adduser --no-create-home -u 1000 -D $APPLICATION_USER && \
    # Configure working directory
    mkdir /app && chown -R $APPLICATION_USER /app

USER 1000

# Copy the JAR file
COPY --chown=1000:1000 build/libs/store.jar /app/store.jar
WORKDIR /app

# Switch to root user
USER root

# Install necessary packages
RUN apk update && \
    apk upgrade && \
    apk add --no-cache bash vim procps htop tzdata libc6-compat && \
    # Set the timezone
    ln -snf /usr/share/zoneinfo/Africa/Johannesburg /etc/localtime && \
    echo "Africa/Johannesburg" > /etc/timezone

USER 1000

# Expose the application port
EXPOSE 8080

# Set the entrypoint to run the application with Java 24 specific flags
ENTRYPOINT ["java", "--enable-preview", "-XX:+UseZGC", "-Xmx512m", "-XX:+ExitOnOutOfMemoryError", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/tmp/", "-XX:OnOutOfMemoryError='echo OOM occurred at $(date) >> /tmp/oom.log'", "-Djdk.virtualThreadScheduler.parallelism=16", "-Djdk.virtualThreadScheduler.maxPoolSize=256", "-jar", "/app/store.jar"]