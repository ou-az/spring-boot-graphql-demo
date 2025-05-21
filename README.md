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
| Product Service (UI) | http://localhost:8081 | Product web UI |
| User Service | http://localhost:8082/api/auth | Authentication endpoints |
| Kafka UI | http://localhost:8083/kafka-ui | Kafka event monitoring dashboard |
| Eureka Dashboard | http://localhost:8761 | Service discovery dashboard |

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

### GraphQL Implementation

- **Schema-first approach** with `.graphqls` files
- **DataFetchers/Resolvers** using Spring's `@SchemaMapping`, `@QueryMapping`, etc.
- **N+1 problem** addressed with DataLoaders for related entities
- **Subscription support** using WebSocket
- **Error handling** with GraphQL-specific error responses

### Microservices Architecture

- **Service discovery** with Eureka
- **API Gateway** with Spring Cloud Gateway
- **Circuit breakers** for resilience
- **Distributed configuration**

### Kafka Integration

- **Event publishing** from services
- **Event consumption** with Spring Kafka listeners
- **Real-time UI** for Kafka event monitoring

## Security Implementation

- **JWT authentication** with Spring Security
- **Role-based access control** for GraphQL operations
- **Method-level security** with `@PreAuthorize`

## Production Considerations

For a production environment, consider:

1. **Persistent databases** instead of H2
2. **Distributed configuration** with Spring Cloud Config
3. **Resilience patterns** with Spring Cloud Circuit Breaker
4. **Monitoring and tracing** with Spring Boot Actuator, Prometheus, and Zipkin
5. **Kafka cluster** with multiple brokers for high availability
6. **CI/CD pipeline** for automated testing and deployment
