# Testing Strategy

## Overview

This document outlines the comprehensive testing strategy for the Spring Boot GraphQL Demo application. It covers various testing levels and approaches, from unit testing to end-to-end testing, with a focus on ensuring reliability in a microservices architecture.

## Testing Pyramid

The application follows the testing pyramid approach:

```
                    ┌───────────┐
                    │    E2E    │
                    │   Tests   │
                    └───────────┘
                   ┌─────────────┐
                   │ Integration │
                   │    Tests    │
                   └─────────────┘
                 ┌───────────────────┐
                 │   Component Tests  │
                 └───────────────────┘
               ┌─────────────────────────┐
               │       Unit Tests        │
               └─────────────────────────┘
```

## Unit Testing

Unit tests verify the functionality of individual components in isolation.

### Service Layer Testing

```java
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @InjectMocks
    private ProductService productService;
    
    @Test
    public void testCreateProduct() {
        // Arrange
        ProductInput input = new ProductInput("Test Product", "Description", 10.0, "CATEGORY");
        Product product = new Product();
        product.setId("1");
        product.setName("Test Product");
        product.setPrice(10.0);
        
        when(productRepository.save(any(Product.class))).thenReturn(product);
        
        // Act
        Product result = productService.createProduct(input);
        
        // Assert
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(10.0, result.getPrice());
        
        // Verify Kafka event was sent
        verify(kafkaTemplate).send(eq("product-events"), any(ProductEvent.class));
    }
    
    @Test
    public void testCreateProductWithKafkaDisabled() {
        // Test with Kafka disabled to verify conditional logic
        ReflectionTestUtils.setField(productService, "kafkaEnabled", false);
        
        // Arrange and Act similar to above
        
        // Verify no Kafka interaction
        verifyNoInteractions(kafkaTemplate);
    }
}
```

### Repository Layer Testing

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;
    
    @Test
    public void testSaveProduct() {
        // Arrange
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(10.0);
        
        // Act
        Product savedProduct = productRepository.save(product);
        
        // Assert
        assertNotNull(savedProduct.getId());
        assertEquals("Test Product", savedProduct.getName());
    }
    
    @Test
    public void testFindByCategory() {
        // Arrange - create and save products with categories
        
        // Act - find by category
        List<Product> products = productRepository.findByCategory("ELECTRONICS");
        
        // Assert
        assertFalse(products.isEmpty());
        assertEquals("ELECTRONICS", products.get(0).getCategory());
    }
}
```

### GraphQL Resolver Testing

```java
@ExtendWith(MockitoExtension.class)
public class ProductGraphQLControllerTest {

    @Mock
    private ProductService productService;
    
    @InjectMocks
    private ProductGraphQLController controller;
    
    @Test
    public void testAllProducts() {
        // Arrange
        List<Product> products = Arrays.asList(
            new Product("1", "Product 1", 10.0),
            new Product("2", "Product 2", 20.0)
        );
        when(productService.getAllProducts()).thenReturn(products);
        
        // Act
        List<Product> result = controller.allProducts();
        
        // Assert
        assertEquals(2, result.size());
        assertEquals("Product 1", result.get(0).getName());
    }
}
```

## Component Testing

Component tests verify the functionality of a specific service or component with its immediate dependencies.

### GraphQL API Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class ProductGraphQLApiTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    public void testQueryAllProducts() throws Exception {
        // Arrange
        List<Product> products = Arrays.asList(
            new Product("1", "Product 1", 10.0),
            new Product("2", "Product 2", 20.0)
        );
        when(productService.getAllProducts()).thenReturn(products);
        
        // Act & Assert
        mockMvc.perform(post("/graphql")
            .content("{\"query\":\"{ allProducts { id name price } }\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.allProducts[0].id").value("1"))
            .andExpect(jsonPath("$.data.allProducts[0].name").value("Product 1"))
            .andExpect(jsonPath("$.data.allProducts[1].id").value("2"));
    }
    
    @Test
    public void testMutationCreateProduct() throws Exception {
        // Arrange
        Product product = new Product("1", "New Product", 15.0);
        when(productService.createProduct(any())).thenReturn(product);
        
        // Act & Assert
        mockMvc.perform(post("/graphql")
            .content("{\"query\":\"mutation { createProduct(product: {name: \\\"New Product\\\", price: 15.0, category: \\\"BOOKS\\\"}) { id name price } }\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.createProduct.id").value("1"))
            .andExpect(jsonPath("$.data.createProduct.name").value("New Product"));
    }
}
```

### REST API Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private JwtUtils jwtUtils;
    
    @Test
    public void testRegisterUser() throws Exception {
        // Arrange
        SignupRequest request = new SignupRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password");
        
        when(userService.registerUser(any())).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }
}
```

### Kafka Consumer Testing

```java
@SpringBootTest
public class ProductEventConsumerTest {

    @Autowired
    private ProductEventConsumer consumer;
    
    @MockBean
    private ProductRepository productRepository;
    
    @Test
    public void testConsumeProductEvent() {
        // Arrange
        ProductEvent event = new ProductEvent("CREATED", "1");
        ConsumerRecord<String, ProductEvent> record = new ConsumerRecord<>(
            "product-events", 0, 0, "key", event
        );
        Acknowledgment ack = mock(Acknowledgment.class);
        
        // Act
        consumer.consumeProductEvent(record, ack);
        
        // Assert
        verify(ack).acknowledge();
        // Additional verifications based on what the consumer does with events
    }
}
```

## Integration Testing

Integration tests verify interactions between multiple components or services.

### Database Integration Testing

```java
@SpringBootTest
@Testcontainers
public class ProductRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    public void testProductCRUD() {
        // Create
        Product product = new Product();
        product.setName("Integration Test Product");
        product.setPrice(25.0);
        
        Product saved = productRepository.save(product);
        assertNotNull(saved.getId());
        
        // Read
        Optional<Product> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        
        // Update
        found.get().setPrice(30.0);
        Product updated = productRepository.save(found.get());
        assertEquals(30.0, updated.getPrice());
        
        // Delete
        productRepository.delete(updated);
        assertFalse(productRepository.findById(updated.getId()).isPresent());
    }
}
```

### Kafka Integration Testing

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"product-events"})
public class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private ProductService productService;
    
    @SpyBean
    private ProductEventConsumer eventConsumer;
    
    @Test
    public void testKafkaProducerConsumer() throws Exception {
        // Arrange
        ProductInput input = new ProductInput("Kafka Test Product", "Description", 15.0, "BOOKS");
        
        // Act
        Product product = productService.createProduct(input);
        
        // Assert - verify consumer received the event
        verify(eventConsumer, timeout(5000).times(1))
            .consumeProductEvent(
                argThat(record -> 
                    record.value().getEventType().equals("CREATED") &&
                    record.value().getProductId().equals(product.getId())
                ),
                any()
            );
    }
}
```

### Service-to-Service Integration

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserProductIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Test
    public void testAuthenticatedProductCreation() {
        // Login and get token
        LoginRequest loginRequest = new LoginRequest("admin", "admin");
        ResponseEntity<JwtResponse> loginResponse = restTemplate.postForEntity(
            "/api/auth/signin", loginRequest, JwtResponse.class);
        
        String token = loginResponse.getBody().getToken();
        
        // Create product with authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        String createProductMutation = "{\"query\":\"mutation { createProduct(product: {name: \\\"Auth Test\\\", price: 25.0, category: \\\"BOOKS\\\"}) { id name } }\"}";
        HttpEntity<String> requestEntity = new HttpEntity<>(createProductMutation, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/graphql", HttpMethod.POST, requestEntity, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Auth Test"));
    }
}
```

## End-to-End Testing

End-to-end tests verify the entire system's functionality from a user perspective.

### Docker Compose Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class DockerComposeEndToEndTest {

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
        new File("src/test/resources/docker-compose-test.yml"))
        .withExposedService("discovery-service", 8761)
        .withExposedService("gateway-service", 8080)
        .withExposedService("user-service", 8082)
        .withExposedService("product-service", 8081)
        .withExposedService("kafka", 9092);
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.serviceUrl.defaultZone", 
            () -> String.format("http://%s:%d/eureka/", 
                environment.getServiceHost("discovery-service", 8761),
                environment.getServicePort("discovery-service", 8761)));
        
        // Additional properties
    }
    
    @Test
    public void testFullUserJourney() {
        // Register user
        // Login and get token
        // Create product
        // Query products
        // Verify all steps work together
    }
}
```

### Cucumber BDD Testing

```java
@CucumberContextConfiguration
@SpringBootTest
public class CucumberTestSteps {

    @Autowired
    private WebTestClient webTestClient;
    
    private String authToken;
    private String createdProductId;
    
    @Given("a user is authenticated")
    public void userIsAuthenticated() {
        // Login and store token
    }
    
    @When("the user creates a product with name {string} and price {double}")
    public void createProduct(String name, double price) {
        // Create product and store ID
    }
    
    @Then("the product appears in the product list")
    public void verifyProductInList() {
        // Query products and verify
    }
}
```

## Performance Testing

Performance tests verify the system's ability to handle load and identify bottlenecks.

### JMeter Test Plan

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0">
  <hashTree>
    <TestPlan>
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments"/>
      </elementProp>
      <stringProp name="TestPlan.comments"></stringProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <boolProp name="TestPlan.functional_test">false</boolProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <elementProp name="ThreadGroup.main_controller" elementType="LoopController">
          <boolProp name="LoopController.continue_forever">false</boolProp>
          <stringProp name="LoopController.loops">10</stringProp>
        </elementProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy>
          <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
            <collectionProp name="Arguments.arguments">
              <elementProp name="" elementType="HTTPArgument">
                <stringProp name="Argument.value">{"query": "{allProducts{id name price}}"}</stringProp>
                <stringProp name="Argument.metadata">=</stringProp>
                <boolProp name="HTTPArgument.use_equals">true</boolProp>
              </elementProp>
            </collectionProp>
          </elementProp>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/graphql</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
          <stringProp name="HTTPSampler.contentEncoding">UTF-8</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### Gatling Performance Test

```scala
class ProductApiSimulation extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
  
  val scn = scenario("GraphQL Product API")
    .exec(http("Get All Products")
      .post("/graphql")
      .body(StringBody("""{"query":"{allProducts{id name price}}"}"""))
      .check(status.is(200))
    )
    .pause(1)
    .exec(http("Get Single Product")
      .post("/graphql")
      .body(StringBody("""{"query":"{productById(id:\"1\"){id name price}}"}"""))
      .check(status.is(200))
    )
  
  setUp(
    scn.inject(
      rampUsers(500).during(60.seconds)
    ).protocols(httpProtocol)
  )
}
```

## Security Testing

Security tests verify the system's resistance to various attacks.

### OWASP ZAP Integration

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class OwaspZapSecurityTest {

    @Test
    public void runZapScan() throws Exception {
        // Initialize ZAP API client
        ClientApi zapClient = new ClientApi("localhost", 8090);
        
        // Spider the application
        String targetUrl = "http://localhost:8080";
        zapClient.spider.scan(targetUrl, null, true, null);
        
        // Wait for spider to complete
        int progress;
        do {
            Thread.sleep(1000);
            progress = Integer.parseInt(zapClient.spider.status(""));
        } while (progress < 100);
        
        // Run active scan
        zapClient.ascan.scan(targetUrl, "true", "false", null, null, null);
        
        // Wait for scan to complete
        do {
            Thread.sleep(1000);
            progress = Integer.parseInt(zapClient.ascan.status(""));
        } while (progress < 100);
        
        // Get alerts
        List<Alert> alerts = zapClient.getAlerts(targetUrl, 0, 0);
        
        // Filter high risks
        List<Alert> highRisks = alerts.stream()
            .filter(alert -> alert.getRisk() >= Alert.Risk.HIGH)
            .collect(Collectors.toList());
        
        // Assert no high risks
        assertTrue(highRisks.isEmpty(), "High security risks found: " + highRisks);
    }
}
```

### JWT Authentication Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/graphql")
            .content("{\"query\":\"mutation { createProduct(product: {name: \\\"Test\\\", price: 10.0}) { id } }\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    public void testAuthorizedAccess() throws Exception {
        // Generate valid token
        String token = jwtUtils.generateTokenFromUsername("admin");
        
        mockMvc.perform(post("/graphql")
            .header("Authorization", "Bearer " + token)
            .content("{\"query\":\"mutation { createProduct(product: {name: \\\"Test\\\", price: 10.0}) { id } }\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
    
    @Test
    public void testInvalidToken() throws Exception {
        mockMvc.perform(post("/graphql")
            .header("Authorization", "Bearer invalid-token")
            .content("{\"query\":\"mutation { createProduct(product: {name: \\\"Test\\\", price: 10.0}) { id } }\"}")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }
}
```

## Containerized Testing

Tests that verify Docker container behavior and network connectivity.

### Container Healthcheck Testing

```java
@SpringBootTest
@Testcontainers
public class ContainerHealthTest {

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("spring-boot-graphql-demo-discovery-service")
        .withExposedPorts(8761)
        .waitingFor(Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
    
    @Container
    static GenericContainer<?> productService = new GenericContainer<>("spring-boot-graphql-demo-product-service")
        .withExposedPorts(8081)
        .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", 
            "http://discovery-service:8761/eureka/")
        .withNetwork(Network.SHARED)
        .waitingFor(Wait.forHttp("/actuator/health")
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(2)));
    
    @Test
    public void testContainersAreHealthy() {
        assertTrue(discoveryService.isRunning());
        assertTrue(productService.isRunning());
        
        // Test container connectivity
        ExecResult result = productService.execInContainer(
            "wget", "-qO-", "http://discovery-service:8761/actuator/health");
        
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("UP"));
    }
}
```

### Network Connectivity Testing

```java
@SpringBootTest
@Testcontainers
public class NetworkConnectivityTest {

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
        new File("docker-compose.yml"))
        .withExposedService("product-service", 8081)
        .withExposedService("user-service", 8082)
        .withExposedService("kafka", 9092);
    
    @Test
    public void testServiceConnectivity() throws Exception {
        GenericContainer<?> productService = environment.getContainerByServiceName("product-service_1").get();
        GenericContainer<?> userService = environment.getContainerByServiceName("user-service_1").get();
        
        // Test product service can reach user service
        ExecResult result = productService.execInContainer(
            "wget", "-qO-", "http://user-service:8082/actuator/health");
        
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("UP"));
        
        // Test Kafka connectivity
        ExecResult kafkaResult = productService.execInContainer(
            "nc", "-zv", "kafka", "9092");
        
        assertEquals(0, kafkaResult.getExitCode());
    }
}
```

## Continuous Integration Testing

Tests integrated into CI/CD pipeline to ensure quality at each stage.

### GitHub Actions Test Workflow

```yaml
name: CI Tests

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Build and Run Unit Tests
      run: mvn test
      
    - name: Run Integration Tests
      run: mvn verify -DskipUnitTests
      
    - name: Build Docker Images
      run: docker-compose build
      
    - name: Start Services
      run: docker-compose up -d
      
    - name: Wait for Services
      run: |
        sleep 60
        docker ps
        
    - name: Run API Tests
      run: |
        curl -X POST http://localhost:8080/graphql \
          -H "Content-Type: application/json" \
          -d '{"query":"{allProducts{id name price}}"}'
        
    - name: Stop Services
      run: docker-compose down
```

## Test Data Management

Strategies for managing test data across different test environments.

### Test Data Fixtures

```java
@Configuration
public class TestDataConfig {

    @Bean
    @Profile("test")
    public CommandLineRunner testDataLoader(ProductRepository productRepository,
                                         CategoryRepository categoryRepository) {
        return args -> {
            // Create categories
            Category electronics = new Category("ELECTRONICS", "Electronic items");
            Category books = new Category("BOOKS", "Books and publications");
            categoryRepository.saveAll(Arrays.asList(electronics, books));
            
            // Create products
            List<Product> products = Arrays.asList(
                new Product("Test Laptop", "A laptop for testing", 999.99, electronics),
                new Product("Test Phone", "A phone for testing", 499.99, electronics),
                new Product("Test Book", "A book for testing", 19.99, books)
            );
            productRepository.saveAll(products);
        };
    }
}
```

### Database Reset Between Tests

```java
@SpringBootTest
public class ProductAPIIntegrationTest {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @BeforeEach
    public void setup() {
        // Clear existing data
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        
        // Create fresh test data
        Category category = new Category("TEST", "Test Category");
        categoryRepository.save(category);
        
        Product product = new Product("Test Product", "Description", 10.0, category);
        productRepository.save(product);
    }
    
    // Tests that use fresh data each time
}
```

## Testing Tools and Libraries

The project uses various testing tools and libraries:

1. **JUnit 5**: Core testing framework
2. **Mockito**: Mocking framework
3. **TestContainers**: Container integration testing
4. **Spring Test**: Spring Boot testing utilities
5. **REST Assured**: API testing
6. **JMeter/Gatling**: Performance testing
7. **Cucumber**: BDD testing
8. **OWASP ZAP**: Security testing
9. **Selenium/Playwright**: UI testing
10. **Pitest**: Mutation testing

## Test Coverage

The project maintains comprehensive test coverage:

```java
@Configuration
public class JacocoConfig {

    @Bean
    public JacocoConfigurationExtension jacocoConfiguration() {
        return new JacocoConfigurationExtension(
            80.0, // Line coverage minimum
            80.0, // Branch coverage minimum
            new String[] {
                "**/model/**",
                "**/dto/**",
                "**/config/**"
            }
        );
    }
}
```

## Conclusion

The testing strategy for this Spring Boot GraphQL Demo demonstrates comprehensive testing practices essential for enterprise applications, particularly for microservices architectures with event-driven components. It includes:

- Unit testing of all application layers
- Component testing of GraphQL and REST APIs
- Integration testing with databases and Kafka
- End-to-end testing of the entire system
- Performance testing for scalability verification
- Security testing for vulnerability detection
- Containerized testing for deployment validation

These patterns showcase testing expertise relevant for senior Java developer roles, especially those requiring experience with modern microservices architectures, event-driven systems, and containerized deployments.
