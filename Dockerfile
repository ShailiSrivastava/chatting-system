# Production Multi-Stage Dockerfile for Render Deployment

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Production Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy executable jar from builder stage
COPY --from=builder /app/target/chat-server.jar app.jar

# Create storage directory and assign permissions
RUN mkdir -p /app/server_storage && chown -R appuser:appgroup /app

USER appuser

# Default port configuration for Render
ENV PORT=10000
EXPOSE 10000 8080 8888

# Optimized JVM memory parameters for Render free tier (512MB RAM)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"

# Entrypoint using dynamic $PORT
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
