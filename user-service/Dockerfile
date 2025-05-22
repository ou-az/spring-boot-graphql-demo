FROM eclipse-temurin:17-jdk as build

# Set the working directory
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .
COPY src ./src
COPY parent-pom.xml ../pom.xml

# Download dependencies and build
RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Run the application with docker profile and additional H2 console settings
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Dspring.h2.console.settings.web-allow-others=true", "-Dspring.h2.console.settings.trace=true", "app.jar"]
