# Use OpenJDK 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy Maven build JAR into container
COPY target/example-0.0.1-SNAPSHOT.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Set environment variable (Gemini API key)
ENV GEMINI_API_KEY=${GEMINI_API_KEY}

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
