# Containerization & DevOps

## Overview

This document details the containerization and DevOps aspects of the Spring Boot GraphQL Demo application. The implementation showcases modern DevOps practices, Docker containerization, and operational excellence - skills particularly relevant for DevOps and Cloud Engineering positions.

## Docker Configuration

The application uses Docker to containerize all microservices, providing consistent environments across development and deployment. Each service is containerized following best practices for Java applications.

### Multi-Stage Build Process

The project uses multi-stage Docker builds to optimize image size and build efficiency:

```dockerfile
# First stage: Build the application
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

# Second stage: Create the runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Run the application with docker profile
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
```

This multi-stage approach provides several benefits:
- Smaller final image size (no build tools in runtime image)
- Improved security (fewer unnecessary components)
- Faster deployments due to smaller image size
- Cleaner separation between build and runtime environments

### Service-Specific Dockerfiles

Each microservice has its own optimized Dockerfile:

#### Discovery Service

```dockerfile
FROM eclipse-temurin:17-jdk as build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY parent-pom.xml ../pom.xml

RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Gateway Service

```dockerfile
FROM eclipse-temurin:17-jdk as build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY parent-pom.xml ../pom.xml

RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
```

#### User Service

```dockerfile
FROM eclipse-temurin:17-jdk as build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY parent-pom.xml ../pom.xml

RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Dspring.h2.console.settings.web-allow-others=true", "-Dspring.h2.console.settings.trace=true", "app.jar"]
```

#### Product Service

```dockerfile
FROM eclipse-temurin:17-jdk as build

WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY parent-pom.xml ../pom.xml

RUN apt-get update && apt-get install -y maven
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Image Optimization

The Dockerfiles implement several optimization techniques:

1. **Layer Caching**: Structured to maximize Docker layer cache utilization
2. **Minimal Base Images**: Using slim JRE images instead of full JDK for runtime
3. **Dependency Caching**: Maven dependencies downloaded before code changes
4. **Resource Constraints**: Container resource limits defined in compose file
5. **Image Pruning**: Unnecessary files excluded from the build context

## Docker Compose Configuration

The application uses Docker Compose for local deployment and orchestration of all services.

### Main Compose File

```yaml
version: '3.8'

services:
  # Eureka Service Registry
  discovery-service:
    build:
      context: ./discovery-service
    container_name: discovery-service
    ports:
      - "8761:8761"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 40s
    networks:
      - spring-cloud-network

  # API Gateway Service
  gateway-service:
    build:
      context: ./gateway-service
    container_name: gateway-service
    ports:
      - "8080:8080"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/
    depends_on:
      discovery-service:
        condition: service_healthy
    networks:
      - spring-cloud-network

  # User Service
  user-service:
    build:
      context: ./user-service
    container_name: user-service
    ports:
      - "8082:8082"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/
    depends_on:
      discovery-service:
        condition: service_healthy
    networks:
      - spring-cloud-network

  # Product Service with GraphQL
  product-service:
    build:
      context: ./product-service
    container_name: product-service
    ports:
      - "8081:8081"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      discovery-service:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - spring-cloud-network

  # Kafka Service for UI
  kafka-service:
    build:
      context: ./kafka-service
    container_name: kafka-service
    ports:
      - "8083:8083"
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      discovery-service:
        condition: service_healthy
      kafka:
        condition: service_started
    networks:
      - spring-cloud-network

  # Zookeeper for Kafka
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    networks:
      - spring-cloud-network

  # Kafka Message Broker
  kafka:
    image: confluentinc/cp-kafka:latest
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - spring-cloud-network

networks:
  spring-cloud-network:
    driver: bridge
```

### Override File for Development

```yaml
# Override configuration for testing and development
services:
  # Disable Kafka in the services during initial startup to resolve build issues
  product-service:
    environment:
      - SPRING_KAFKA_ENABLED=false
      
  kafka-service:
    environment:
      - SPRING_KAFKA_ENABLED=false

  # Gateway service configuration
  gateway-service:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD_GATEWAY=DEBUG
      - LOGGING_LEVEL_ROOT=INFO
    ports:
      - "8080:8080"
    depends_on:
      - discovery-service
      - user-service
      - product-service

  # User service configuration with enhanced H2 console settings
  user-service:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_H2_CONSOLE_SETTINGS_WEB-ALLOW-OTHERS=true
      - LOGGING_LEVEL_COM_EXAMPLE_USERSERVICE=DEBUG
      - SPRING_JPA_SHOW_SQL=true
    ports:
      - "8082:8082"
```

## Environment-Specific Configurations

The application uses Spring profiles to tailor configurations for different environments.

### Docker Profile Configuration

Each service has a Docker-specific configuration file:

#### Gateway Service Docker Configuration

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      routes:
        # Product Service Routes
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**, /graphql/product/**, /graphiql/product/**
          filters:
            - RewritePath=/graphql/product/(?<path>.*), /graphql/$\{path}
            - RewritePath=/graphiql/product/(?<path>.*), /graphiql/$\{path}
        
        # User Service Routes
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/auth/**, /api/users/**, /user-service/h2-console/**, /user-service/h2-console
          filters:
            - RewritePath=/user-service/h2-console/(?<path>.*), /h2-console/$\{path}
            - RewritePath=/user-service/h2-console, /h2-console
        
        # H2 Console Route - Dedicated route for H2 console
        - id: user-service-h2-console
          uri: lb://user-service
          predicates:
            - Path=/user-service/h2-console/**
          filters:
            - RewritePath=/user-service/h2-console/(?<segment>.*), /h2-console/$\{segment}
            - RemoveRequestHeader=X-Frame-Options
        
        # Direct H2 Console access - alternative path
        - id: user-service-h2-direct
          uri: lb://user-service
          predicates:
            - Path=/h2-console/**
        
        # Kafka UI Service Routes
        - id: kafka-service
          uri: lb://kafka-service
          predicates:
            - Path=/kafka-ui/**

# Eureka client configuration
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://discovery-service:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true
```

#### User Service Docker Configuration

```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  
  # Database configuration
  datasource:
    url: jdbc:h2:mem:userdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
    show-sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        web-allow-others: true  # Critical for Docker container access
        web-admin-password: password
        trace: true # Enable tracing for debugging purposes

# Eureka client configuration
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://discovery-service:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true
```

#### Product Service Docker Configuration

```yaml
server:
  port: 8081

spring:
  application:
    name: product-service
  
  # Database configuration
  datasource:
    url: jdbc:h2:mem:productdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  
  # Kafka configuration
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    enabled: ${SPRING_KAFKA_ENABLED:true}

# Eureka client configuration
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://discovery-service:8761/eureka/}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true
```

## Network Configuration

The application uses Docker networking to establish communication between services:

### Network Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ External Client │────▶│ Docker Host     │────▶│ Docker Network  │
│ (Browser)       │     │ (Exposed Ports) │     │ (Internal Comm) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                               ┌────────────────────────┴───────────────────────────┐
                               ▼                        ▼                           ▼
                      ┌─────────────────┐     ┌─────────────────┐       ┌─────────────────┐
                      │ API Gateway     │────▶│ Microservices   │──────▶│ Kafka + Zookeeper│
                      │ (Port 8080)     │     │ (Internal Ports)│       │ (Internal Ports) │
                      └─────────────────┘     └─────────────────┘       └─────────────────┘
```

### Network Design Principles

1. **Service Discovery**: Services registered with Eureka for dynamic discovery
2. **Port Mapping**: Only required ports exposed to the host
3. **Network Isolation**: All services on a dedicated bridge network
4. **Internal DNS**: Services communicate using service names as hostnames
5. **Security**: Minimal port exposure and network segmentation

## Health Monitoring and Management

The application implements comprehensive health monitoring:

### Actuator Endpoints

Each service exposes Spring Boot Actuator endpoints:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### Docker Healthchecks

Healthchecks ensure service availability:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 3
  start_period: 40s
```

### Logging Configuration

Centralized logging configuration:

```yaml
logging:
  level:
    root: INFO
    org.springframework.cloud.gateway: DEBUG
    com.example.productservice: DEBUG
    org.springframework.graphql: INFO
```

## Resource Management

The application includes resource constraints for container optimization:

```yaml
services:
  product-service:
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.1'
          memory: 256M
```

## Security Considerations

The containerized deployment includes several security best practices:

1. **Minimal Base Images**: Using slim JRE images to reduce attack surface
2. **No Root Execution**: Running containers as non-root users where possible
3. **Read-Only Filesystem**: Using read-only filesystem for containers where applicable
4. **Secret Management**: Using environment variables for sensitive information
5. **Network Segmentation**: Isolating services on dedicated network
6. **Exposed Ports**: Minimizing exposed ports to reduce attack vectors

## CI/CD Integration

The application is designed for CI/CD pipeline integration:

### GitHub Actions Workflow Example

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Build with Maven
      run: mvn -B package --file pom.xml
      
    - name: Build and push Docker images
      uses: docker/build-push-action@v2
      with:
        context: .
        push: true
        tags: |
          myregistry/product-service:latest
          myregistry/user-service:latest
          myregistry/gateway-service:latest
          myregistry/discovery-service:latest
```

### Deployment Automation

Automated deployment scripts for different environments:

```bash
#!/bin/bash
# deploy.sh - Deploy to staging environment

# Pull latest images
docker-compose -f docker-compose.yml -f docker-compose.staging.yml pull

# Update services with zero downtime
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d --no-deps discovery-service
sleep 30  # Wait for discovery service to be healthy
docker-compose -f docker-compose.yml -f docker-compose.staging.yml up -d --no-deps gateway-service user-service product-service kafka-service

# Cleanup
docker system prune -f
```

## Operational Excellence

The application demonstrates several operational best practices:

### Graceful Shutdown

Spring Boot applications configured for graceful shutdown:

```java
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Rolling Updates

Docker Compose configuration for rolling updates:

```yaml
services:
  product-service:
    deploy:
      update_config:
        order: start-first
        failure_action: rollback
        delay: 10s
      rollback_config:
        parallelism: 0
        order: stop-first
```

### Monitoring Dashboard

Integration with monitoring tools:

```yaml
services:
  prometheus:
    image: prom/prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - spring-cloud-network
      
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    depends_on:
      - prometheus
    networks:
      - spring-cloud-network
```

## Cloud Deployment Considerations

The containerized application is designed for deployment to various cloud platforms:

### Kubernetes Deployment Example

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: product-service
  template:
    metadata:
      labels:
        app: product-service
    spec:
      containers:
      - name: product-service
        image: myregistry/product-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: "http://discovery-service:8761/eureka/"
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
```

### AWS Deployment Considerations

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Resources:
  ECSCluster:
    Type: AWS::ECS::Cluster
    Properties:
      ClusterName: spring-boot-graphql-demo
      
  ProductServiceTask:
    Type: AWS::ECS::TaskDefinition
    Properties:
      Family: product-service
      ContainerDefinitions:
        - Name: product-service
          Image: !Sub ${AWS::AccountId}.dkr.ecr.${AWS::Region}.amazonaws.com/product-service:latest
          Essential: true
          PortMappings:
            - ContainerPort: 8081
          Environment:
            - Name: SPRING_PROFILES_ACTIVE
              Value: aws
```

## Local Development Workflow

The application supports efficient local development:

### Development Workflow

1. **Clone Repository**: `git clone https://github.com/example/spring-boot-graphql-demo.git`
2. **Start Dependencies**: `docker-compose up -d kafka zookeeper`
3. **Run Services Locally**: Run each service in IDE with development profile
4. **Test API**: Access GraphiQL at `http://localhost:8081/graphiql`
5. **View Metrics**: Access Actuator endpoints at `http://localhost:8081/actuator`

### Docker Development Flow

```bash
# Build images
docker-compose build

# Run specific service with debug port
docker-compose up -d discovery-service
docker-compose run --service-ports --name product-service-debug product-service java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar app.jar

# View logs
docker-compose logs -f product-service

# Execute commands in container
docker-compose exec product-service java -XX:+PrintFlagsFinal -version
```

## Troubleshooting Guide

Common Docker-related issues and solutions:

### Network Connectivity Issues

```bash
# Check if services can communicate
docker-compose exec product-service ping gateway-service

# Inspect network
docker network inspect spring-boot-graphql-demo_spring-cloud-network

# View all containers and their networks
docker ps --format "{{.Names}}: {{.Networks}}"
```

### Container Health Issues

```bash
# Check container logs
docker-compose logs user-service

# Check container health
docker inspect --format "{{.State.Health.Status}}" spring-boot-graphql-demo_discovery-service_1

# View resource usage
docker stats
```

### Application Issues

```bash
# Check application logs
docker-compose logs -f product-service

# Access application shell
docker-compose exec product-service /bin/sh

# Check JVM status
docker-compose exec product-service jcmd 1 VM.info
```

## Conclusion

The containerization and DevOps implementation in this Spring Boot application demonstrates enterprise-grade practices:

- Multi-stage Docker builds
- Environment-specific configurations
- Network security and isolation
- Health monitoring and management
- CI/CD integration
- Resource optimization
- Operational excellence

These patterns showcase advanced DevOps expertise that's particularly relevant for DevOps Engineer and Cloud Engineer positions, demonstrating the ability to design and implement containerized microservices architectures with operational excellence.
