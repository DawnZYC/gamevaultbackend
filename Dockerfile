# ==========================================
# Multi-Stage Docker Build for GameVault Backend
# Based on Workshop 4 - Containers Best Practices
# ==========================================

# ==========================================
# Stage 1: Build Stage (Maven Build)
# ==========================================
FROM maven:3.9.9-eclipse-temurin-17 AS builder
LABEL maintainer="GameVault Team"
LABEL stage="builder"

WORKDIR /app

# Copy only pom.xml first (for dependency caching)
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests in Docker build, tests run in CI)
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Runtime Stage (Lightweight JRE)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="GameVault Team"
LABEL description="GameVault Backend - Spring Boot Application"
LABEL version="1.0.0"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for file storage and secrets
RUN mkdir -p /app/uploads /app/secrets/keys && \
    chown -R appuser:appgroup /app

# Copy secrets (RSA keys for JWT)
COPY --chown=appuser:appgroup secrets/keys /app/secrets/keys

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]