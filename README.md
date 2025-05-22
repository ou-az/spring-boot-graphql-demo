# Spring Boot GraphQL Demo with Microservices and Kafka

This project demonstrates a comprehensive implementation of GraphQL in a Spring Boot microservices architecture with Kafka integration. It showcases best practices for GraphQL implementation in enterprise applications and provides a practical example for interview discussions.

## Architecture Overview

The project consists of the following microservices:

- **Product Service**: Implements GraphQL API for product management with JPA data access
- **User Service**: Provides JWT-based authentication and authorization
- **Gateway Service**: API Gateway using Spring Cloud Gateway for routing requests
- **Kafka Service**: Kafka event monitoring with real-time UI dashboard
- **Discovery Service**: Eureka service registry for service discovery

## Key Features

- **GraphQL API**: Full implementation with queries, mutations, and subscriptions
- **Authentication & Security**: JWT-based security with role-based access
- **Event-Driven Architecture**: Kafka integration for publishing and consuming events
- **Real-time Updates**: WebSocket support for GraphQL subscriptions and Kafka event monitoring
- **Microservices**: Service discovery, API gateway, and distributed architecture
- **Docker**: Containerization of all services with Docker Compose for local deployment

## Technology Stack

- **Java 17**: Modern Java features
- **Spring Boot 3.2.0**: Latest Spring Boot version
- **Spring for GraphQL**: Official Spring GraphQL implementation
- **Spring Cloud**: Microservices support with Eureka and Gateway
- **Spring Security**: Authentication and authorization
- **Spring Data JPA**: Database access with Hibernate
- **Apache Kafka**: Event streaming platform
- **H2 Database**: In-memory database for local development
- **WebSocket**: Real-time communication
- **Docker**: Containerization

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- Docker and Docker Compose (for running with containers)

### Running Locally (Without Docker)

1. Clone the repository:
   ```bash
   git clone https://github.com/ou-az/spring-boot-graphql-demo.git
   cd spring-boot-graphql-demo
   ```

2. Build all services:
   ```bash
   mvn clean package -DskipTests
   ```

3. Start the services in the following order:
   - Discovery Service (Eureka)
   - User Service
   - Product Service
   - Kafka Service
   - Gateway Service

   For each service, navigate to its directory and run:
   ```bash
   cd <service-directory>
   mvn spring-boot:run
   ```

### Running with Docker Compose

1. Clone the repository:
   ```bash
   git clone https://github.com/ou-az/spring-boot-graphql-demo.git
   cd spring-boot-graphql-demo
   ```

2. Build and start all services:
   ```bash
   docker-compose up --build
   ```

## Accessing the Services

| Service | URL | Description |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | API Gateway entry point |
| Product Service (GraphQL) | http://localhost:8081/graphql | GraphQL API endpoint |
| Product Service (GraphiQL) | http://localhost:8081/graphiql | GraphQL interactive UI |
| Product Service (H2 Console) | http://localhost:8081/h2-console | Database management console |
| User Service | http://localhost:8082/api/auth | Authentication endpoints |
| User Service (H2 Console) | http://localhost:8082/h2-console | User database console |
| Kafka UI | http://localhost:8083/kafka-ui | Kafka event monitoring dashboard |
| Eureka Dashboard | http://localhost:8761 | Service discovery dashboard |

### H2 Console Access

To access the H2 database consoles in the containerized environment:

- **Product Service H2 Console**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:productdb`
  - Username: `sa`
  - Password: `password`

- **User Service H2 Console**: http://localhost:8082/h2-console
  - JDBC URL: `jdbc:h2:mem:userdb`
  - Username: `sa`
  - Password: `password`

## GraphQL API Usage

### GraphiQL Interface

The easiest way to explore the GraphQL API is through the GraphiQL interface at http://localhost:8081/graphiql

### Sample GraphQL Queries

#### Query All Products
```graphql
query {
  products {
    id
    name
    description
    price
    stockQuantity
    category {
      name
    }
    createdAt
  }
}
```

#### Query a Single Product
```graphql
query {
  product(id: "1") {
    id
    name
    description
    price
    category {
      name
    }
  }
}
```

### Sample GraphQL Mutations

#### Create a Product (requires authentication)
```graphql
mutation {
  createProduct(input: {
    name: "New Product"
    description: "This is a new product created via GraphQL"
    price: 29.99
    stockQuantity: 100
    categoryId: "1"
  }) {
    id
    name
    price
  }
}
```

#### Update a Product (requires authentication)
```graphql
mutation {
  updateProduct(
    id: "1", 
    input: {
      name: "Updated Product"
      price: 39.99
      stockQuantity: 150
      categoryId: "1"
    }
  ) {
    id
    name
    price
  }
}
```

### GraphQL Subscriptions (WebSocket)

The application supports real-time updates via GraphQL subscriptions for product events:

```graphql
subscription {
  productCreated {
    id
    name
    price
  }
}
```

## Authentication

The User Service provides JWT-based authentication:

1. Register a new user:
   ```
   POST http://localhost:8082/api/auth/signup
   ```
   ```json
   {
     "username": "newuser",
     "email": "user@example.com",
     "password": "password",
     "roles": ["user"]
   }
   ```

2. Login to get a JWT token:
   ```
   POST http://localhost:8082/api/auth/signin
   ```
   ```json
   {
     "username": "newuser",
     "password": "password"
   }
   ```

3. Use the JWT token in the Authorization header for protected endpoints:
   ```
   Authorization: Bearer <jwt_token>
   ```

## Project Structure

```
spring-boot-graphql-demo/
├── product-service/             # GraphQL API and data access
├── user-service/                # Authentication and user management
├── gateway-service/             # API Gateway
├── kafka-service/               # Kafka monitoring UI
├── discovery-service/           # Eureka service registry
├── docker-compose.yml           # Docker Compose configuration
└── README.md                    # Project documentation
```

## Key Implementation Details

### GraphQL Implementation (Java Development Skills)

- **Schema-first approach** with `.graphqls` files
- **DataFetchers/Resolvers** using Spring's `@SchemaMapping`, `@QueryMapping`, etc.
- **N+1 problem** addressed with DataLoaders for related entities
- **Subscription support** using WebSocket
- **Error handling** with GraphQL-specific error responses
- **Comprehensive exception handling** with custom error responses

### Microservices Architecture (System Design Skills)

- **Service discovery** with Eureka for dynamic service registration
- **API Gateway** with Spring Cloud Gateway for centralized routing
- **Circuit breakers** for fault tolerance and resilience
- **Distributed configuration** with environment-specific profiles
- **Service-to-service communication** with REST and messaging
- **Containerized deployment** with Docker and Docker Compose

### Kafka Integration (Streaming Data Skills)

- **Event publishing** from services with conditional execution
- **Event consumption** with Spring Kafka listeners and error handling
- **Message schema design** with JSON serialization/deserialization
- **Real-time event processing** with consumer groups
- **Resilient error handling** with dead-letter topics and retry mechanisms
- **Scalable architecture** for high-throughput event processing

### DevOps and Containerization

- **Docker multi-stage builds** for optimized container images
- **Container orchestration** with Docker Compose
- **Environment-specific configurations** for development and production
- **Network configuration** for inter-service communication
- **Resource optimization** for containerized services
- **Security considerations** for containerized applications

## Security Implementation

- **JWT authentication** with Spring Security
- **Role-based access control** for GraphQL operations
- **Method-level security** with `@PreAuthorize`
- **Cross-Origin Resource Sharing (CORS)** configuration
- **Content Security Policy** implementation
- **Header security** with protection against XSS and CSRF attacks

## Production Considerations

This demo showcases several enterprise-ready patterns, but for a full production environment, consider these additional enhancements:

### High Availability & Scalability
1. **Database Clustering**: Replace H2 with PostgreSQL/MySQL clusters
2. **Kafka Cluster**: Configure multi-broker Kafka deployment with replication
3. **Service Redundancy**: Deploy multiple instances of each microservice
4. **Load Balancing**: Implement server-side load balancing with Ribbon/Spring Cloud LoadBalancer
5. **Autoscaling**: Configure Kubernetes horizontal pod autoscaling based on metrics

### Observability & Monitoring
1. **Distributed Tracing**: Implement with Spring Cloud Sleuth and Zipkin
2. **Metrics Collection**: Use Micrometer with Prometheus for detailed metrics
3. **Centralized Logging**: Configure ELK stack (Elasticsearch, Logstash, Kibana)
4. **Health Monitoring**: Enhanced actuator endpoints with custom health indicators
5. **Alerting**: Configure alerts based on predefined thresholds

### Security Enhancements
1. **API Rate Limiting**: Implement throttling to prevent abuse
2. **Advanced Authentication**: Add OAuth2/OpenID Connect with external providers
3. **Secrets Management**: Use Vault or Kubernetes secrets for sensitive data
4. **Security Scanning**: Integrate dependency vulnerability scanning
5. **Compliance Validation**: Implement data protection measures (GDPR, etc.)

### DevOps & CI/CD
1. **Infrastructure as Code**: Define all infrastructure with Terraform/CloudFormation
2. **CI/CD Pipeline**: Implement with GitHub Actions, Jenkins, or GitLab CI
3. **Blue/Green Deployments**: Zero-downtime deployment strategy
4. **Automated Testing**: Comprehensive unit, integration, and performance tests
5. **Environment Parity**: Consistent configurations across development, staging, and production
