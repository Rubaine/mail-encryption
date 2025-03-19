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
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Installation des dépendances requises
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Copie des fichiers de l'application
COPY --from=build /build/target/mail-encryption-with-dependencies.jar /app/app.jar
COPY params /app/params

# Création des répertoires de logs
RUN mkdir -p /app/logs

# Exposition du port
EXPOSE 8080

# Définition de la commande de démarrage
CMD ["java", "-jar", "/app/app.jar"]
