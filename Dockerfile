FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Run application - bind to 0.0.0.0
ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-jar", "/app/app.jar"]
