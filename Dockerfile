# Build the API, then run only the packaged jar.
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --create-home --shell /usr/sbin/nologin springuser

COPY --from=builder /app/target/futuremediabackend-*.jar app.jar
RUN chown springuser:springuser app.jar

USER springuser

EXPOSE 8980

ENV SERVER_PORT=8980
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "app.jar"]
