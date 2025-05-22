# GraphQL Implementation

## Overview

This document details the GraphQL implementation in the Spring Boot GraphQL Demo application. The implementation showcases modern Java development practices and advanced GraphQL concepts, making it particularly relevant for Java-focused roles such as Lead Java Software Engineer positions.

## GraphQL Schema Design

The application follows a schema-first approach, defining the GraphQL schema in `.graphqls` files. This approach provides several advantages:

- Clear contract definition for API consumers
- Type safety and validation
- Self-documenting API design
- Separation of concerns between schema and implementation

### Schema Structure

The GraphQL schema is organized into the following components:

#### 1. Types

```graphql
type Product {
  id: ID!
  name: String!
  description: String
  price: Float!
  category: Category
  createdAt: String!
  updatedAt: String
}

type Category {
  id: ID!
  name: String!
  description: String
}
```

#### 2. Inputs

```graphql
input ProductInput {
  name: String!
  description: String
  price: Float!
  categoryId: ID!
}

input ProductUpdateInput {
  name: String
  description: String
  price: Float
  categoryId: ID
}
```

#### 3. Queries

```graphql
type Query {
  allProducts: [Product!]!
  productById(id: ID!): Product
  productsByCategory(category: String!): [Product!]!
  categories: [Category!]!
}
```

#### 4. Mutations

```graphql
type Mutation {
  createProduct(product: ProductInput!): Product!
  updateProduct(id: ID!, input: ProductUpdateInput!): Product!
  deleteProduct(id: ID!): Boolean!
}
```

#### 5. Subscriptions

```graphql
type Subscription {
  productCreated: Product!
  productUpdated: Product!
  productDeleted: ID!
}
```

### Schema Location

The schema files are located in `product-service/src/main/resources/graphql/` and include:
- `schema.graphqls`: Main schema definition
- `product.graphqls`: Product-related types and operations
- `category.graphqls`: Category-related types and operations

## Resolver Implementation

GraphQL resolvers are implemented using Spring for GraphQL's annotation-based approach. This demonstrates modern Java development practices with annotation-driven configuration.

### Controller-Based Resolvers

The application uses Spring's `@Controller` classes with GraphQL-specific annotations to implement resolvers:

```java
@Controller
public class ProductGraphQLController {

    private final ProductService productService;

    @Autowired
    public ProductGraphQLController(ProductService productService) {
        this.productService = productService;
    }

    @QueryMapping
    public List<Product> allProducts() {
        return productService.getAllProducts();
    }

    @QueryMapping
    public Product productById(@Argument String id) {
        return productService.getProductById(id)
                .orElse(null);
    }

    @MutationMapping
    public Product createProduct(@Argument ProductInput product) {
        return productService.createProduct(product);
    }
    
    // Additional query and mutation handlers...
}
```

### Field Resolvers

Field resolvers handle relationship fields using `@SchemaMapping`:

```java
@Controller
public class CategoryGraphQLController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryGraphQLController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @SchemaMapping(typeName = "Product", field = "category")
    public Category getCategory(Product product) {
        return categoryService.getCategoryById(product.getCategoryId())
                .orElse(null);
    }
}
```

## GraphQL Subscriptions

Subscriptions provide real-time updates using WebSocket, demonstrating event-driven programming with reactive streams.

### Subscription Implementation

```java
@Controller
public class ProductSubscriptionController {

    private final Sinks.Many<ProductEvent> productEventSink;
    
    @Autowired
    public ProductSubscriptionController(Sinks.Many<ProductEvent> productEventSink) {
        this.productEventSink = productEventSink;
    }

    @SubscriptionMapping
    public Flux<Product> productCreated() {
        return productEventSink.asFlux()
            .filter(event -> event.getEventType().equals("CREATED"))
            .map(event -> productService.getProductById(event.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found")));
    }
    
    // Additional subscription handlers...
}
```

### Subscription Configuration

WebSocket support is configured in Spring Boot:

```java
@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketHandler webSocketHandler(WebSocketHandler webSocketHandler) {
        return webSocketHandler;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(WebSocketHandler webSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/graphql-ws", webSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(-1);
        return handlerMapping;
    }
}
```

## DataFetcher Optimization

Several optimizations are implemented to ensure efficient GraphQL execution:

### 1. DataLoader Pattern

DataLoaders are used to solve the N+1 query problem by batching and caching related entity fetches:

```java
@Component
public class CategoryDataLoader {

    private final CategoryRepository categoryRepository;
    
    @Bean
    public DataLoader<String, Category> categoryDataLoader() {
        return DataLoader.newDataLoader(ids -> {
            List<Category> categories = categoryRepository.findAllById(ids);
            Map<String, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
            return CompletableFuture.supplyAsync(() -> 
                ids.stream()
                    .map(id -> categoryMap.getOrDefault(id, null))
                    .collect(Collectors.toList())
            );
        });
    }
}
```

### 2. Selective Field Fetching

The service layer is optimized to fetch only required fields based on GraphQL selection sets:

```java
@Service
public class ProductService {

    // Other methods...

    public List<Product> getProductsByCategory(String category, DataFetchingFieldSelectionSet selectionSet) {
        boolean includeCategory = selectionSet.contains("category");
        
        return productRepository.findByCategory(category).stream()
            .map(product -> {
                if (!includeCategory) {
                    product.setCategory(null); // Don't fetch if not requested
                }
                return product;
            })
            .collect(Collectors.toList());
    }
}
```

### 3. Pagination Implementation

GraphQL pagination follows the Relay cursor specification:

```graphql
type ProductConnection {
  edges: [ProductEdge!]!
  pageInfo: PageInfo!
}

type ProductEdge {
  node: Product!
  cursor: String!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

type Query {
  # Other queries...
  productsConnection(first: Int, after: String): ProductConnection!
}
```

```java
@QueryMapping
public ProductConnection productsConnection(
        @Argument Integer first, 
        @Argument String after) {
    
    return productService.getProductsConnection(first, after);
}
```

## Error Handling

The GraphQL API implements comprehensive error handling to provide clear feedback to clients:

### 1. GraphQL Errors

```java
@ExceptionHandler(ProductNotFoundException.class)
public GraphQLError handleProductNotFound(ProductNotFoundException ex) {
    return GraphQLError.newError()
        .message(ex.getMessage())
        .errorType(ErrorType.NOT_FOUND)
        .build();
}
```

### 2. Custom Error Types

```java
public enum CustomErrorType implements ErrorType {
    PRODUCT_NOT_FOUND,
    VALIDATION_ERROR,
    UNAUTHORIZED,
    FORBIDDEN
}
```

### 3. Validation Errors

```java
@ExceptionHandler(ConstraintViolationException.class)
public GraphQLError handleConstraintViolation(ConstraintViolationException ex) {
    List<ValidationError> validationErrors = ex.getConstraintViolations()
        .stream()
        .map(violation -> new ValidationError(
            violation.getPropertyPath().toString(),
            violation.getMessage()))
        .collect(Collectors.toList());
            
    return GraphQLError.newError()
        .message("Validation errors")
        .errorType(CustomErrorType.VALIDATION_ERROR)
        .extensions(Collections.singletonMap("validationErrors", validationErrors))
        .build();
}
```

## Security Integration

GraphQL operations are secured with Spring Security integration:

### 1. Authorization Checks

```java
@MutationMapping
@PreAuthorize("hasRole('ADMIN')")
public Product deleteProduct(@Argument String id) {
    return productService.deleteProduct(id);
}
```

### 2. Custom Security Directives

```graphql
directive @requireAuth on FIELD_DEFINITION
directive @hasRole(role: String!) on FIELD_DEFINITION

type Mutation {
  createProduct(product: ProductInput!): Product! @requireAuth
  updateProduct(id: ID!, input: ProductUpdateInput!): Product! @hasRole(role: "ADMIN")
}
```

```java
@Component
public class SecurityDirectiveWiring implements SchemaDirectiveWiring {

    @Override
    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition field = environment.getElement();
        
        if (environment.getDirective("requireAuth") != null) {
            // Implement authentication check
        }
        
        if (environment.getDirective("hasRole") != null) {
            String role = environment.getDirective("hasRole").getArgument("role").getValue();
            // Implement role-based authorization
        }
        
        return field;
    }
}
```

## Testing GraphQL API

The GraphQL API is comprehensively tested with different approaches:

### 1. Unit Testing Resolvers

```java
@WebMvcTest(ProductGraphQLController.class)
public class ProductGraphQLControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    public void testGetAllProducts() throws Exception {
        // Test setup and execution
        when(productService.getAllProducts()).thenReturn(Arrays.asList(
            new Product("1", "Test Product", 10.0)
        ));
        
        // GraphQL query execution
        mockMvc.perform(post("/graphql")
            .content("{\"query\": \"{allProducts{id name price}}\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.allProducts[0].id").value("1"))
            .andExpect(jsonPath("$.data.allProducts[0].name").value("Test Product"));
    }
}
```

### 2. Integration Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ProductGraphQLIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    public void testCreateProduct() {
        String query = "mutation { createProduct(product: " +
            "{name: \"Test Product\", price: 10.0, categoryId: \"1\"}) " +
            "{id name price}}";
            
        webTestClient.post().uri("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Collections.singletonMap("query", query))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.createProduct.name").isEqualTo("Test Product")
            .jsonPath("$.data.createProduct.price").isEqualTo(10.0);
    }
}
```

### 3. Subscription Testing

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ProductSubscriptionTest {

    @Autowired
    private WebSocketClient webSocketClient;
    
    @Test
    public void testProductCreatedSubscription() {
        // Test subscription handling
    }
}
```

## Performance Considerations

Several strategies are implemented to ensure GraphQL API performance:

### 1. Query Complexity Analysis

```java
@Configuration
public class GraphQLConfig {

    @Bean
    public GraphQLSchema schema() {
        return SchemaParser.newParser()
            .file("schema.graphqls")
            .resolvers(/* resolvers */)
            .directive("complexity", new ComplexityDirective())
            .build()
            .makeExecutableSchema();
    }
    
    @Bean
    public Instrumentation complexityInstrumentation() {
        return new ComplexityInstrumentation(
            ComplexityInstrumentationOptions.newOptions()
                .maxComplexity(100)
                .build()
        );
    }
}
```

### 2. Query Depth Limiting

```java
@Bean
public Instrumentation maxQueryDepthInstrumentation() {
    return new MaxQueryDepthInstrumentation(10);
}
```

### 3. Result Caching

```java
@Bean
public DataLoaderRegistry dataLoaderRegistry() {
    DataLoaderRegistry registry = new DataLoaderRegistry();
    
    DataLoader<String, Category> categoryLoader = DataLoader.newDataLoader(
        categoryBatchLoader, 
        DataLoaderOptions.newOptions().setCachingEnabled(true)
    );
    
    registry.register("categoryLoader", categoryLoader);
    return registry;
}
```

## Tools and Libraries

The GraphQL implementation leverages several key libraries:

1. **Spring for GraphQL**: Core GraphQL integration with Spring Boot
2. **GraphQL Java**: Underlying GraphQL Java implementation
3. **Reactor**: For reactive programming with subscriptions
4. **Project Reactor**: For reactive streams and subscription handling
5. **Jackson**: For JSON serialization/deserialization
6. **Spring Security**: For securing GraphQL operations

## Conclusion

The GraphQL implementation in this Spring Boot application demonstrates advanced Java development practices, including:

- Modern annotation-based configuration
- Reactive programming with WebFlux and Project Reactor
- Integration with Spring Security
- Comprehensive error handling
- Performance optimization techniques
- Testing strategies for GraphQL APIs

These patterns showcase Java expertise that's particularly relevant for senior Java developer roles, especially those requiring experience with modern API design and GraphQL implementation.
