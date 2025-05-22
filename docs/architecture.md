# Architecture Overview

## System Architecture

The Spring Boot GraphQL Demo is built on a modern microservices architecture that demonstrates enterprise-level design patterns and best practices. This architecture leverages several key technologies:

- **Spring Boot**: Core framework for all microservices
- **Spring Cloud**: For microservice orchestration and service discovery
- **GraphQL**: For flexible and efficient API access
- **Kafka**: For event-driven communication between services
- **Docker**: For containerization and deployment

### High-Level Architecture Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Web Browser   │────▶│   API Gateway   │────▶│  Auth Service   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │ │
                 ┌─────────────┘ └──────────────┐
                 ▼                              ▼
        ┌─────────────────┐             ┌─────────────────┐
        │ Product Service │◀───Kafka───▶│   User Service  │
        └─────────────────┘             └─────────────────┘
                 │                              │
                 ▼                              ▼
        ┌─────────────────┐             ┌─────────────────┐
        │  Product DB (H2)│             │   User DB (H2)  │
        └─────────────────┘             └─────────────────┘
                 │
                 ▼
        ┌─────────────────┐
        │ Kafka Monitoring│
        └─────────────────┘
```

## Microservices Design

The application is composed of the following microservices, each with a specific responsibility:

### 1. Discovery Service (Port 8761)

**Responsibility**: Service registration and discovery using Netflix Eureka.

**Key Features**:
- Centralized registry for all microservices
- Health monitoring for registered services
- Dynamic service discovery
- Load balancing support

**Implementation Details**:
- Uses Spring Cloud Netflix Eureka Server
- Configured with standalone mode for demonstration
- Includes dashboard for monitoring service status

### 2. Gateway Service (Port 8080)

**Responsibility**: API Gateway that routes requests to appropriate microservices.

**Key Features**:
- Centralized routing
- Path-based routing to microservices
- Cross-cutting concerns (logging, security)
- Request transformation and response aggregation

**Implementation Details**:
- Uses Spring Cloud Gateway
- Configured with reactive programming model
- Includes security filters for authentication
- Implements CORS configuration
- Custom route configurations for GraphQL and H2 console access

### 3. User Service (Port 8082)

**Responsibility**: User management, authentication, and authorization.

**Key Features**:
- User registration and management
- JWT token generation and validation
- Role-based access control
- Profile management

**Implementation Details**:
- Spring Security with JWT implementation
- H2 in-memory database for user storage
- RESTful API for user operations
- Integration with Gateway for authentication

### 4. Product Service (Port 8081)

**Responsibility**: Product management with GraphQL API.

**Key Features**:
- GraphQL API for product operations
- Event publishing to Kafka
- GraphQL subscriptions for real-time updates
- Data persistence with JPA

**Implementation Details**:
- Spring for GraphQL implementation
- H2 in-memory database for product storage
- Integration with Kafka for event publishing
- WebSocket support for GraphQL subscriptions

### 5. Kafka Service (Port 8083)

**Responsibility**: Kafka monitoring and management UI.

**Key Features**:
- Kafka cluster monitoring
- Topic management
- Message inspection
- Performance metrics

**Implementation Details**:
- Custom Kafka UI dashboard
- Integration with Kafka cluster
- Real-time monitoring capabilities

## Communication Patterns

The architecture employs multiple communication patterns to demonstrate different integration approaches:

### 1. Synchronous Communication (REST/GraphQL)

- Used for direct service-to-service communication
- Gateway to User Service for authentication
- Client to Gateway for API access
- Gateway to Product Service for GraphQL operations

### 2. Asynchronous Communication (Kafka)

- Used for event-driven communication
- Product Service publishes events to Kafka topics
- Services consume events from Kafka topics
- Enables eventual consistency and loose coupling

### 3. WebSocket Communication

- Used for real-time updates
- GraphQL subscriptions for live data updates
- Kafka monitoring for real-time event visualization

## Data Flow Diagrams

### Authentication Flow

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  Client │────▶│ Gateway │────▶│  User   │────▶│  User   │
│         │     │ Service │     │ Service │     │   DB    │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
     ▲               │               │
     └───────────────┴───────────────┘
            JWT Token Response
```

### GraphQL Query Flow

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  Client │────▶│ Gateway │────▶│ Product │────▶│ Product │
│         │     │ Service │     │ Service │     │   DB    │
└─────────┘     └─────────┘     └─────────┘     └─────────┘
     ▲               │               │
     └───────────────┴───────────────┘
            GraphQL Response
```

### Event Publishing Flow

```
┌─────────┐     ┌─────────┐     ┌─────────┐
│ Product │────▶│  Kafka  │────▶│ Service │
│ Service │     │ Cluster │     │Consumers│
└─────────┘     └─────────┘     └─────────┘
                     │
                     ▼
               ┌─────────┐
               │  Kafka  │
               │ Service │
               └─────────┘
```

## Design Principles

The architecture adheres to several key design principles:

### 1. Single Responsibility Principle

Each microservice has a specific and well-defined responsibility.

### 2. Loose Coupling

Services communicate through well-defined interfaces (REST, GraphQL) or message brokers (Kafka), minimizing direct dependencies.

### 3. High Cohesion

Related functionality is grouped together within each microservice.

### 4. Resilience by Design

Services are designed to handle failures gracefully with circuit breakers, timeouts, and retry mechanisms.

### 5. Observability

The architecture includes comprehensive logging, monitoring, and health checks.

### 6. Security in Depth

Security is implemented at multiple layers (Gateway, Service, Database).

## Technology Stack Justification

### Spring Boot

Selected for its robust features, rapid development capabilities, and excellent integration with other Spring ecosystem components. Spring Boot's auto-configuration and starter dependencies significantly reduce boilerplate code and configuration.

### GraphQL

Chosen over traditional REST API for:
- Precise data fetching (no over/under fetching)
- Single endpoint for multiple operations
- Strong typing with schema definition
- Real-time capabilities with subscriptions
- Introspection and self-documentation

### Kafka

Selected for asynchronous communication due to:
- High throughput event processing
- Reliable message delivery
- Message persistence
- Scalability for high-volume event processing
- Support for event sourcing and CQRS patterns

### Docker

Used for containerization to:
- Ensure consistent environments
- Simplify deployment
- Enable scaling of individual services
- Facilitate CI/CD integration
- Provide isolation between services

## Scalability Considerations

The architecture is designed with scalability in mind:

### Horizontal Scaling

- All services can be scaled horizontally
- Stateless design enables multiple instances
- Service discovery supports dynamic scaling

### Database Scaling

- Current implementation uses H2 for simplicity
- Can be replaced with scalable databases like PostgreSQL or MongoDB
- Sharding strategies can be implemented for high-volume data

### Kafka Scaling

- Kafka cluster can scale with additional brokers
- Partitioning enables parallel processing
- Consumer groups allow load distribution

## Future Architecture Enhancements

### Service Mesh

Integration with Istio or Linkerd for:
- Advanced traffic management
- Fine-grained security policies
- Enhanced observability

### Serverless Components

Implementing serverless functions for:
- Event processing
- Scheduled tasks
- Batch processing

### Multi-Region Deployment

Enhancing for global distribution:
- Cross-region replication
- Data locality
- Geographic load balancing

## Architecture Decision Records (ADRs)

### ADR-001: GraphQL Over REST

**Context**: Need for flexible API that minimizes over-fetching and under-fetching.

**Decision**: Use GraphQL as the primary API for the Product Service.

**Consequences**: More complex server-side implementation but greater client flexibility and efficiency.

### ADR-002: JWT for Authentication

**Context**: Need for stateless authentication in a distributed system.

**Decision**: Implement JWT-based authentication.

**Consequences**: Enables stateless authentication, but requires secure token management.

### ADR-003: Kafka for Event Communication

**Context**: Need for reliable asynchronous communication between services.

**Decision**: Use Kafka for event-driven communication.

**Consequences**: More complex setup but enables loose coupling and scalable event processing.

### ADR-004: Containerization with Docker

**Context**: Need for consistent deployment across environments.

**Decision**: Containerize all services with Docker and Docker Compose.

**Consequences**: More complex local setup but consistent environment and simplified deployment.

### ADR-005: H2 Database for Development

**Context**: Need for simple database setup for development and testing.

**Decision**: Use H2 in-memory database.

**Consequences**: Simplified setup but not suitable for production. Designed to be easily replaced with production databases.
