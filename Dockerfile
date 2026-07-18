# Production-ready Dockerfile for Google Cloud Run deployment.
FROM eclipse-temurin:21-jre-jammy

# Set active profile to production
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

WORKDIR /app

# Copy compiled jar to target container
COPY target/*.jar app.jar

EXPOSE 8080

# Execute Spring Boot backend on run
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]
