# Use the Maven image to build the application
FROM maven:3.8.3-openjdk-17-slim AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files (pom.xml and the rest of the app)
COPY pom.xml ./
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Now create the runtime image
FROM openjdk:17-jdk-slim AS runtime

# Set the working directory for the Spring Boot app
WORKDIR /app

# Copy the built JAR from the previous stage into the container
COPY --from=build /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]