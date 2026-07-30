# ============================
# 1️⃣ Build Stage
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for faster build)
RUN mvn clean package -DskipTests

# ============================
# 2️⃣ Runtime Stage
# ============================
FROM eclipse-temurin:21-jre

# Working directory
WORKDIR /app

# Create non-root user
RUN useradd -ms /bin/bash springuser

# Copy built JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create upload directories (matching FileStorageService subfolders)
RUN mkdir -p \
    /app/uploads/news \
    /app/uploads/events \
    /app/uploads/gallery \
    /app/uploads/vacancies \
    /app/uploads/applications \
    /app/uploads/banners

# Assign permissions
RUN chown -R springuser:springuser /app
USER springuser

# Expose the port from application.properties
EXPOSE 8980

# Environment variables (can be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8980
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

# Volume for file uploads
VOLUME ["/app/uploads"]

# Start the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
