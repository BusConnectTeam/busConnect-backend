# Stage 1: Build dependencies
FROM maven:3.9-eclipse-temurin-21-alpine AS dependencies

WORKDIR /app

# Copy only pom files first for better caching
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY catalog-service/pom.xml catalog-service/
COPY eureka-service/pom.xml eureka-service/

# Download all dependencies (this layer is cached unless pom.xml changes)
RUN mvn dependency:go-offline -pl api-gateway -am -B

# Stage 2: Build application
FROM dependencies AS builder

# Copy source code
COPY api-gateway/src api-gateway/src

# Build the application with optimizations
RUN mvn clean package -pl api-gateway -am -DskipTests -B && \
    mkdir -p /app/extracted && \
    java -Djarmode=layertools -jar /app/api-gateway/target/*.jar extract --destination /app/extracted

# Stage 3: Optimized Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for healthcheck (más ligero que wget)
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# Copy extracted layers for better caching and faster startup
COPY --from=builder --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/extracted/application/ ./

USER spring

# Health check optimizado
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
