# Kafka Integration

## Overview

This document details the Kafka integration in the Spring Boot GraphQL Demo application. The implementation showcases real-time data streaming and event-driven architecture - skills particularly relevant for process streaming engineer positions. This section demonstrates advanced Kafka configuration, producer/consumer implementations, and error handling strategies.

## Event-Driven Architecture

The application implements an event-driven architecture using Apache Kafka as the messaging backbone. This architectural style enables:

- Loose coupling between services
- Asynchronous communication
- Improved scalability
- Event sourcing capabilities
- Real-time data processing

### Event Flow Diagram

```
┌───────────────┐     ┌─────────────┐     ┌────────────────┐
│ Product       │────▶│ Kafka       │────▶│ Event          │
│ Service       │     │ Topics      │     │ Consumers      │
└───────────────┘     └─────────────┘     └────────────────┘
                           │                      │
                           ▼                      ▼
                     ┌─────────────┐       ┌────────────────┐
                     │ Kafka       │       │ Subscription   │
                     │ Monitoring  │       │ WebSockets     │
                     └─────────────┘       └────────────────┘
```

## Kafka Configuration

The Kafka infrastructure is configured with production-ready settings while maintaining flexibility for development environments.

### Base Configuration

```java
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        if (!kafkaEnabled) {
            return null;
        }
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic productTopic() {
        if (!kafkaEnabled) {
            return null;
        }
        return TopicBuilder.name("product-events")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000)) // 7 days
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
```

### Producer Configuration

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        if (!kafkaEnabled) {
            return null;
        }
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        if (!kafkaEnabled) {
            return null;
        }
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### Consumer Configuration

```java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        if (!kafkaEnabled) {
            return null;
        }
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.productservice.event");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        if (!kafkaEnabled) {
            return null;
        }
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setErrorHandler(kafkaErrorHandler());
        return factory;
    }
    
    @Bean
    public ErrorHandler kafkaErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10000L);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
```

## Event Models

Events are modeled as Java classes with clear serialization/deserialization support:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventType;  // CREATED, UPDATED, DELETED
    private String productId;
    private Instant timestamp = Instant.now();
    
    public ProductEvent(String eventType, String productId) {
        this.eventType = eventType;
        this.productId = productId;
    }
}
```

## Producer Implementation

The Kafka producer implementation demonstrates best practices for event publishing:

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    
    private static final String PRODUCT_TOPIC = "product-events";
    
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;
    
    public Product createProduct(ProductInput productInput) {
        // Create product logic...
        Product savedProduct = productRepository.save(product);
        
        // Publish event to Kafka
        if (kafkaEnabled) {
            ProductEvent event = new ProductEvent("CREATED", savedProduct.getId());
            kafkaTemplate.send(PRODUCT_TOPIC, event);
            log.info("Sent product created event for product id: {}", savedProduct.getId());
        } else {
            log.info("Kafka disabled: Skipping product created event for product id: {}", savedProduct.getId());
        }
        
        return savedProduct;
    }
    
    // Additional methods with event publishing...
}
```

### Transaction Support

The application demonstrates transactional event publishing to ensure consistency:

```java
@Service
@Transactional
public class TransactionalProductService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ProductRepository productRepository;
    
    @Transactional
    public Product createProductWithTransaction(ProductInput input) {
        // Save product to database
        Product product = convertInputToProduct(input);
        Product savedProduct = productRepository.save(product);
        
        // Send event within the same transaction
        kafkaTemplate.send("product-events", new ProductEvent("CREATED", savedProduct.getId()));
        
        return savedProduct;
    }
}
```

## Consumer Implementation

The Kafka consumer implementation shows how to process events efficiently:

```java
@Service
@Slf4j
public class ProductEventConsumer {

    private final ProductRepository productRepository;
    private final Sinks.Many<ProductEvent> productEventSink;
    
    @KafkaListener(topics = "product-events", groupId = "product-event-consumer")
    public void consumeProductEvent(ConsumerRecord<String, ProductEvent> record, 
                                    Acknowledgment acknowledgment) {
        ProductEvent event = record.value();
        log.info("Received product event: {}", event);
        
        try {
            // Process the event
            processEvent(event);
            
            // Forward to WebSocket subscribers
            productEventSink.tryEmitNext(event);
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing product event", e);
            // Let the error handler deal with retries/DLT
            throw e;
        }
    }
    
    private void processEvent(ProductEvent event) {
        switch (event.getEventType()) {
            case "CREATED":
                // Handle creation event
                break;
            case "UPDATED":
                // Handle update event
                break;
            case "DELETED":
                // Handle deletion event
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }
}
```

## Error Handling and Recovery

The application implements sophisticated error handling for Kafka operations:

### Dead Letter Topic

```java
@Bean
public NewTopic deadLetterTopic() {
    return TopicBuilder.name("product-events.DLT")
            .partitions(3)
            .replicas(1)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7 * 24 * 60 * 60 * 1000)) // 7 days
            .build();
}
```

### Error Handler Configuration

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> template) {
    // Recoverer that sends to Dead Letter Topic
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
            (record, exception) -> {
                log.error("Failed to process record. Sending to DLT", exception);
                return new TopicPartition(record.topic() + ".DLT", record.partition());
            });
    
    // Exponential backoff for retries
    ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
    backOff.setMaxInterval(10000L); // Max 10 seconds between retries
    backOff.setMaxElapsedTime(60000L); // Retry for up to 1 minute
    
    // Configure error handler with backoff and recoverer
    DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
    
    // Configure which exceptions should not trigger retries
    errorHandler.addNotRetryableExceptions(
        IllegalArgumentException.class,
        JsonParseException.class
    );
    
    return errorHandler;
}
```

### Dead Letter Topic Consumer

```java
@Service
@Slf4j
public class DeadLetterTopicConsumer {

    @KafkaListener(topics = "product-events.DLT", groupId = "dlt-consumer")
    public void consumeDeadLetterEvents(ConsumerRecord<String, Object> record,
                                        Acknowledgment acknowledgment) {
        log.error("Processing dead letter record: {}", record);
        
        // Log details for analysis
        log.error("Failed message key: {}", record.key());
        log.error("Failed message value: {}", record.value());
        log.error("Failed message headers: {}", Arrays.toString(record.headers().toArray()));
        
        // Store in database for manual recovery
        storeFailedMessage(record);
        
        // Acknowledge processing of the dead letter
        acknowledgment.acknowledge();
    }
    
    private void storeFailedMessage(ConsumerRecord<String, Object> record) {
        // Store in database for later analysis and recovery
    }
}
```

## Monitoring and Management

### Kafka Health Indicator

```java
@Component
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final AdminClient adminClient;
    
    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }
    
    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Check if Kafka is reachable
            DescribeClusterResult result = adminClient.describeCluster();
            result.nodes().get(10, TimeUnit.SECONDS);
            
            // Check topic metadata
            ListTopicsResult topics = adminClient.listTopics();
            topics.names().get(10, TimeUnit.SECONDS);
            
            builder.up()
                   .withDetail("status", "Kafka server is reachable");
        } catch (Exception e) {
            builder.down()
                   .withDetail("error", e.getMessage());
        }
    }
}
```

### Kafka Metrics

```java
@Configuration
public class KafkaMetricsConfig {

    @Bean
    public MeterBinder kafkaConsumerMetrics(ConsumerFactory<String, Object> consumerFactory) {
        return registry -> {
            consumerFactory.getMetrics().forEach((name, metric) -> {
                Gauge.builder("kafka.consumer." + name, metric, m -> m.metricValue())
                     .register(registry);
            });
        };
    }
    
    @Bean
    public MeterBinder kafkaProducerMetrics(ProducerFactory<String, Object> producerFactory) {
        return registry -> {
            producerFactory.getMetrics().forEach((name, metric) -> {
                Gauge.builder("kafka.producer." + name, metric, m -> m.metricValue())
                     .register(registry);
            });
        };
    }
}
```

## Testing Kafka Integration

The application includes comprehensive testing for Kafka components:

### Unit Testing Producers

```java
@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Mock
    private ProductRepository productRepository;
    
    @InjectMocks
    private ProductService productService;
    
    @Test
    public void testCreateProductPublishesEvent() {
        // Setup
        ProductInput input = new ProductInput("Test Product", 10.0, "1");
        Product savedProduct = new Product("1", "Test Product", 10.0);
        when(productRepository.save(any())).thenReturn(savedProduct);
        
        // Execute
        productService.createProduct(input);
        
        // Verify
        verify(kafkaTemplate).send(
            eq("product-events"), 
            argThat(event -> {
                ProductEvent productEvent = (ProductEvent) event;
                return productEvent.getEventType().equals("CREATED") &&
                       productEvent.getProductId().equals("1");
            })
        );
    }
}
```

### Integration Testing with Embedded Kafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"product-events"})
public class KafkaIntegrationTest {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @SpyBean
    private ProductEventConsumer eventConsumer;
    
    @Test
    public void testKafkaIntegration() throws Exception {
        // Create product (should publish event)
        ProductInput input = new ProductInput("Test Product", 10.0, "1");
        productService.createProduct(input);
        
        // Verify consumer processes the event
        verify(eventConsumer, timeout(5000)).consumeProductEvent(any(), any());
    }
}
```

### Consumer Testing

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"product-events", "product-events.DLT"})
public class ProductEventConsumerTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @SpyBean
    private ProductEventConsumer eventConsumer;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Test
    public void testEventProcessing() throws Exception {
        // Send test event
        ProductEvent event = new ProductEvent("CREATED", "1");
        kafkaTemplate.send("product-events", event).get();
        
        // Verify consumer processes it
        verify(eventConsumer, timeout(5000)).consumeProductEvent(any(), any());
        
        // Verify business logic was executed
        // (depends on what the consumer does with events)
    }
    
    @Test
    public void testErrorHandling() throws Exception {
        // Setup consumer to throw exception
        doThrow(new RuntimeException("Test exception"))
            .when(eventConsumer).processEvent(any());
        
        // Send test event
        ProductEvent event = new ProductEvent("INVALID", "999");
        kafkaTemplate.send("product-events", event).get();
        
        // Verify event ends up in DLT
        // (requires consumer for DLT topic in test)
    }
}
```

## Conditional Kafka Configuration

The application supports conditional Kafka enablement, demonstrating flexible deployment options:

```java
@Configuration
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {
    // Kafka configuration that only applies when Kafka is enabled
}

@Service
public class ProductService {
    
    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;
    
    public void createProduct(ProductInput input) {
        // Save product
        
        // Conditionally publish event
        if (kafkaEnabled) {
            kafkaTemplate.send("product-events", new ProductEvent("CREATED", product.getId()));
        } else {
            log.info("Kafka disabled: Skipping event publishing");
        }
    }
}
```

## Performance Tuning

The Kafka integration includes performance optimizations for high-throughput scenarios:

### Producer Batching

```java
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    // Other properties...
    
    // Batching configuration
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
    
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

### Consumer Optimization

```java
@Bean
public ConsumerFactory<String, Object> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    // Other properties...
    
    // Performance tuning
    props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
    
    return new DefaultKafkaConsumerFactory<>(props);
}
```

### Parallel Processing

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    
    // Configure parallel processing
    factory.setConcurrency(3); // 3 threads per topic-partition
    factory.setBatchListener(true); // Process messages in batches
    
    return factory;
}

@KafkaListener(topics = "product-events", concurrency = "3")
public void consumeProductEvents(List<ConsumerRecord<String, ProductEvent>> records,
                                Acknowledgment acknowledgment) {
    // Process batch of records
    records.parallelStream().forEach(this::processRecord);
    
    // Acknowledge all at once
    acknowledgment.acknowledge();
}
```

## Conclusion

The Kafka integration in this Spring Boot application demonstrates enterprise-grade event streaming with:

- Production-ready configuration
- Resilient error handling with dead letter topics
- Transactional event publishing
- Comprehensive testing strategies
- Performance optimization techniques
- Monitoring and health checks
- Conditional configuration for flexible deployment

These patterns showcase advanced streaming data expertise that's particularly relevant for Java Developer positions focused on process streaming, high-throughput data processing, and event-driven architectures.
