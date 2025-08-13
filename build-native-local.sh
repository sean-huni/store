#!/usr/bin/env bash

# Script to build the native image locally without Docker

# Ensure GraalVM is installed and configured
if ! command -v native-image &> /dev/null; then
    echo "Error: GraalVM native-image is not installed or not in PATH"
    echo "Please install GraalVM 24+ and the native-image component"
    echo "For example, using SDKMAN:"
    echo "  sdk install java 24.0.2-graalce"
    echo "  sdk use java 24.0.2-graalce"
    echo "  gu install native-image"
    exit 1
fi

# Set memory options for the JVM running Gradle
export JAVA_OPTS="-Xmx12g -XX:MaxMetaspaceSize=2g"
echo "Setting JAVA_OPTS to $JAVA_OPTS"

# Clean and build
echo "Cleaning project..."
./gradlew clean

# Build native image
echo "Building native image..."
./gradlew nativeCompile \
    -Porg.gradle.java.installations.auto-download=false \
    -Dspring.native.mode=reflection \
    -Dspring.native.verbose=true \
    -Dorg.gradle.jvmargs="-Xmx12g -XX:MaxMetaspaceSize=2g" \
    -Dorg.gradle.workers.max=2 \
    -Dorg.gradle.parallel=false \
    -Dorg.graalvm.nativeimage.imagecode=1 \
    -Dorg.graalvm.nativeimage.native-image-args="--gc=epsilon -Xmx1g -Xms1g" \
    --no-daemon

# Check if build was successful
if [ -f "build/native/nativeCompile/store" ]; then
    echo "Native image built successfully at build/native/nativeCompile/store"
    echo "You can run it with: build/native/nativeCompile/store"
    
    # Make it executable
    chmod +x build/native/nativeCompile/store
    
    echo "You can run the native image with:"
    echo "build/native/nativeCompile/store --spring.profiles.active=dev --DB_HOST=localhost:5433 --DB_NAME=store --DB_PASS=postgres --DB_USER=postgres"
else
    echo "Failed to build native image"
    echo "Checking for any executable files in build directory:"
    find build -type f -executable | grep -v "\.jar$" | grep -v "\.sh$"
    exit 1
fi