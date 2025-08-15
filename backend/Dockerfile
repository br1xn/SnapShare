# Build application
FROM maven:3.9.8-eclipse-temurin-22 AS build
WORKDIR /app

# Dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run application
FROM openjdk:22-jdk-slim
WORKDIR /app

# Jar file
COPY --from=build /app/target/file-sharing-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]