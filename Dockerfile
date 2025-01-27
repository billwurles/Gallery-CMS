# Stage 1: Build the Spring Boot application
FROM maven:3.8.6-openjdk-17-slim AS builder
WORKDIR /app

# Copy Maven configuration and project files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot application
RUN mvn clean package -DskipTests

# Stage 2: Set up runtime environment with NGINX and Spring Boot
FROM openjdk:17-jdk-slim AS runtime

# Install NGINX
RUN apt-get update && apt-get install -y nginx && apt-get clean

# Create shared volume for NGINX and Spring Boot
RUN mkdir -p /shared/appdata/html_root

# Copy NGINX configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built Spring Boot JAR
COPY --from=builder /app/target/*.jar /app/app.jar

# Set NGINX to serve static content from the shared directory
VOLUME /shared/appdata/html_root

# Expose ports for NGINX and Spring Boot
EXPOSE 80 8080

# Start NGINX and Spring Boot
CMD service nginx start && java -jar /app/app.jar
