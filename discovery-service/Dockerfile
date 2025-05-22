FROM eclipse-temurin:17-jdk as build

# Set the working directory
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Copy the source code
COPY src ./src

# Copy parent POM
COPY parent-pom.xml ../pom.xml

# Build the application
RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
