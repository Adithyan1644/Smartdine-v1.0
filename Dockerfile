# Stage 1: Compile and package the Java Spring Boot application using Maven
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package the application jar
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Build the final lightweight JRE container image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

# Copy the compiled jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]
