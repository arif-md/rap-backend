# syntax=docker/dockerfile:1
# Multi-stage build for Spring Boot application
# Stage 1: Build stage
# Use a JDK image instead of Maven image since we're using Maven Wrapper
FROM eclipse-temurin:17-jdk-jammy AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Pass -U (via Dev-Build/Dev-Rebuild -ForceUpdate) to make Maven re-check
# remote repos for updated snapshots/releases instead of trusting the cache
ARG MAVEN_UPDATE_FLAG=

# Download dependencies only (cached if pom.xml hasn't changed)
# The BuildKit cache mount persists /root/.m2 across builds independent of
# layer invalidation — dependency:go-offline alone doesn't reliably fetch
# everything a later build needs (plugin deps in particular), so without
# this mount, every source change triggers a fresh set of network fetches
# even when this layer itself is cache-hit.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw dependency:go-offline -B $MAVEN_UPDATE_FLAG

# Copy source code
COPY src ./src

# Build the application. Skip tests in Docker build for faster builds (run
# tests in CI/CD). Uses the same persistent .m2 cache mount as above.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw package -DskipTests -B $MAVEN_UPDATE_FLAG

# Extract layers from the WAR file for optimized Docker layers
WORKDIR /app/target
RUN java -Djarmode=layertools -jar *.war extract

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy

# Add labels for better image management
LABEL maintainer="x.y.z"
LABEL version="0.0.1-SNAPSHOT"
LABEL description="Spring Boot Backend API"

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy extracted layers from builder stage
# This allows Docker to cache layers that don't change often
COPY --from=builder /app/target/dependencies/ ./
COPY --from=builder /app/target/spring-boot-loader/ ./
COPY --from=builder /app/target/snapshot-dependencies/ ./
COPY --from=builder /app/target/application/ ./

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check configuration
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# Entry point using exec form for proper signal handling
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.WarLauncher"]
