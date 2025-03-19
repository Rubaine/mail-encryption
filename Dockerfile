# Use maven image for building - compatible with ARM64
FROM maven:3.8.6-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /build

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download all dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src/ ./src/
COPY params/ ./params/

# Build the project (skip tests) with the shade plugin to create a fat JAR
RUN mvn clean package -DskipTests

# Use ARM64-compatible JDK image for runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat JAR with all dependencies
COPY --from=build /build/target/mail-encryption-with-dependencies.jar /app/mail-encryption.jar
# Copy parameter files (needed for cryptographic operations)
COPY --from=build /build/params/ /app/params/

# Log directory
RUN mkdir -p /app/logs && \
    chmod 777 /app/logs

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Environment variables with defaults
ENV TRUST_AUTHORITY_PORT=8080 \
    DEBUG_MODE=false \
    PAIRING_PARAMETERS_PATH=params/curves/a.properties

# Expose the Trust Authority Server port
EXPOSE ${TRUST_AUTHORITY_PORT}

# Set the entry point to use the fat JAR directly
ENTRYPOINT ["java", "-jar", "mail-encryption.jar"]
