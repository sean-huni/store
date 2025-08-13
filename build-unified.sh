#!/usr/bin/env bash

# Unified Build Script for Store Microservice
# Supports both regular JAR and GraalVM native image builds
# Follows Paketo.io best practices for containerization

set -e  # Exit on any error

# Configuration
APP_NAME="store"
DOCKER_REGISTRY="username"
DOCKERFILE_PATH=".docker/Dockerfile"
DOCKERFILE_NATIVE_PATH=".docker/Dockerfile.native"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Extract version from build.gradle
get_version() {
    if [[ -f "build.gradle" ]]; then
        # Extract version from build.gradle using grep and sed
        local version=$(grep "^version = " build.gradle | sed "s/version = ['\"]//g" | sed "s/['\"].*//g" | tr -d ' ')
        if [[ -n "$version" ]]; then
            echo "$version"
        else
            log_error "Could not extract version from build.gradle"
            exit 1
        fi
    else
        log_error "build.gradle file not found"
        exit 1
    fi
}

# Update version files (if they exist)
update_version_files() {
    local version=$1
    local version_files=("src/main/resources/version.yml" "src/test/resources/version.yml")
    
    for file in "${version_files[@]}"; do
        if [[ -f "$file" ]]; then
            log_info "Updating $file with version $version"
            echo "version: $version" > "$file"
        fi
    done
    
    # Update version.txt if it exists
    if [[ -f "version.txt" ]]; then
        echo "$version" > version.txt
        log_info "Updated version.txt with version $version"
    fi
}

# Clean up Docker images
cleanup_docker_images() {
    local image_pattern=$1
    log_info "Cleaning up existing Docker images matching pattern: $image_pattern"
    
    # Remove existing images (ignore errors if no images found)
    docker images --format "table {{.Repository}}:{{.Tag}}\t{{.ID}}" | grep "$image_pattern" | awk '{print $2}' | xargs -r docker rmi -f 2>/dev/null || true
}

# Build regular JAR
build_jar() {
    local version=$1
    log_info "Building JAR for version $version"
    
    # Clean and build
    ./gradlew clean
    ./gradlew build -x test
    
    # Verify JAR was created
    if [[ -f "build/libs/${APP_NAME}.jar" ]]; then
        log_success "JAR built successfully: build/libs/${APP_NAME}.jar"
    else
        log_error "JAR build failed - ${APP_NAME}.jar not found"
        exit 1
    fi
}

# Build Docker image for regular JAR
build_docker_jar() {
    local version=$1
    local image_name="${APP_NAME}-img"
    
    log_info "Building Docker image for JAR: $image_name:$version"
    
    # Clean up existing images
    cleanup_docker_images "$image_name"
    
    # Build Docker image
    docker build --platform linux/amd64 -f "$DOCKERFILE_PATH" -t "$image_name:$version" .
    
    log_success "Docker image built: $image_name:$version"
}

# Build native image locally (faster but OS-dependent)
build_native_local() {
    local version=$1
    log_info "Building native image locally for version $version"
    
    # Check if GraalVM is available
    if ! command -v native-image &> /dev/null; then
        log_error "GraalVM native-image is not installed or not in PATH"
        log_info "Please install GraalVM 24+ and the native-image component"
        log_info "For example, using SDKMAN:"
        log_info "  sdk install java 24.0.2-graalce"
        log_info "  sdk use java 24.0.2-graalce"
        log_info "  gu install native-image"
        exit 1
    fi
    
    # Set memory options for the JVM running Gradle
    export JAVA_OPTS="-Xmx12g -XX:MaxMetaspaceSize=2g"
    log_info "Setting JAVA_OPTS to $JAVA_OPTS"
    
    # Clean and build native image
    ./gradlew clean
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
    
    # Verify native executable was created
    if [[ -f "build/native/nativeCompile/${APP_NAME}" ]]; then
        chmod +x "build/native/nativeCompile/${APP_NAME}"
        log_success "Native image built successfully: build/native/nativeCompile/${APP_NAME}"
    else
        log_error "Native image build failed"
        exit 1
    fi
}

# Build Docker image for native executable
build_docker_native() {
    local version=$1
    local image_name="${APP_NAME}-native-img"
    
    log_info "Building Docker native image: $image_name:$version"
    
    # Clean up existing images
    cleanup_docker_images "$image_name"
    
    # Build Docker native image
    docker build --platform linux/amd64 -f "$DOCKERFILE_NATIVE_PATH" -t "$image_name:$version" .
    
    log_success "Docker native image built: $image_name:$version"
}

# Deploy locally with Docker
deploy_local() {
    local version=$1
    local native=${2:-false}
    local container_name="${APP_NAME}"
    local image_name="${APP_NAME}-img"
    
    if [[ "$native" == "true" ]]; then
        container_name="${APP_NAME}-native"
        image_name="${APP_NAME}-native-img"
    fi
    
    log_info "Deploying $container_name locally"
    
    # Stop and remove existing container
    docker stop "$container_name" 2>/dev/null || true
    docker rm "$container_name" 2>/dev/null || true
    
    # Run new container
    docker run --name="$container_name" -d -it -p 8080:8080 "$image_name:$version"
    
    log_success "Container $container_name started successfully"
    log_info "Following logs..."
    docker logs "$container_name" -f
}

# Tag and push to registry
tag_and_push() {
    local version=$1
    local native=${2:-false}
    local image_name="${APP_NAME}-img"
    local tag_suffix=""
    
    if [[ "$native" == "true" ]]; then
        image_name="${APP_NAME}-native-img"
        tag_suffix="-native"
    fi
    
    log_info "Tagging and pushing $image_name:$version"
    
    # Git tag
    git tag "$version$tag_suffix"
    git push origin "$version$tag_suffix"
    
    # Docker tag and push
    docker tag "$image_name:$version" "$DOCKER_REGISTRY/$image_name:$version"
    docker push "$DOCKER_REGISTRY/$image_name:$version"
    
    log_success "Tagged and pushed $DOCKER_REGISTRY/$image_name:$version"
}

# Main build function
build() {
    local native=${1:-false}
    local version=$(get_version)
    
    log_info "Starting build process for version $version (native: $native)"
    
    # Update version files
    update_version_files "$version"
    
    if [[ "$native" == "true" ]]; then
        build_docker_native "$version"
    else
        build_jar "$version"
        build_docker_jar "$version"
    fi
    
    log_success "Build completed successfully for version $version"
}

# Complete workflow (build + tag + push)
do_all() {
    local native=${1:-false}
    local version=$(get_version)
    
    build "$native"
    tag_and_push "$version" "$native"
}

# Show usage
usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  get_version              - Extract and display version from build.gradle"
    echo "  build                    - Build JAR and Docker image"
    echo "  build_native             - Build native Docker image"
    echo "  build_native_local       - Build native image locally (faster, OS-dependent)"
    echo "  deploy                   - Deploy regular image locally"
    echo "  deploy_native            - Deploy native image locally"
    echo "  tag_and_push             - Tag and push regular image"
    echo "  tag_and_push_native      - Tag and push native image"
    echo "  do_all                   - Build + tag + push regular image"
    echo "  do_all_native            - Build + tag + push native image"
    echo ""
    echo "Examples:"
    echo "  $0 build                 # Build regular JAR and Docker image"
    echo "  $0 build_native          # Build native Docker image"
    echo "  $0 do_all                # Complete workflow for regular build"
    echo "  $0 do_all_native         # Complete workflow for native build"
}

# Main script logic
case "${1:-}" in
    get_version)
        get_version
        ;;
    build)
        build false
        ;;
    build_native)
        build true
        ;;
    build_native_local)
        build_native_local "$(get_version)"
        ;;
    deploy)
        deploy_local "$(get_version)" false
        ;;
    deploy_native)
        deploy_local "$(get_version)" true
        ;;
    tag_and_push)
        tag_and_push "$(get_version)" false
        ;;
    tag_and_push_native)
        tag_and_push "$(get_version)" true
        ;;
    do_all)
        do_all false
        ;;
    do_all_native)
        do_all true
        ;;
    help|--help|-h)
        usage
        ;;
    "")
        log_error "No command specified"
        usage
        exit 1
        ;;
    *)
        log_error "Unknown command: $1"
        usage
        exit 1
        ;;
esac