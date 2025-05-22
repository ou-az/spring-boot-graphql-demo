package com.example.productservice.service;

import com.example.productservice.event.ProductEvent;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.model.Category;
import com.example.productservice.model.Product;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    
    @Value("${spring.kafka.enabled:true}")
    private boolean kafkaEnabled;
    
    // Sink for GraphQL Subscriptions
    private final Sinks.Many<Product> productCreatedSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<Product> productUpdatedSink = Sinks.many().multicast().onBackpressureBuffer();
    
    // Constants
    private static final String PRODUCT_TOPIC = "product-events";
    
    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<Product> findProductsByCategoryId(Long categoryId) {
        // Verify category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId);
    }
    
    @Transactional
    public Product createProduct(String name, String description, BigDecimal price, Integer stockQuantity, Long categoryId) {
        // Find category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        
        // Create product
        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(price)
                .stockQuantity(stockQuantity)
                .category(category)
                .build();
        
        Product savedProduct = productRepository.save(product);
        
        // Send Kafka event if enabled
        if (kafkaEnabled) {
            ProductEvent event = new ProductEvent("CREATED", savedProduct.getId());
            kafkaTemplate.send(PRODUCT_TOPIC, event);
            log.info("Sent product created event for product id: {}", savedProduct.getId());
        } else {
            log.info("Kafka disabled: Skipping product created event for product id: {}", savedProduct.getId());
        }
        
        // Emit for GraphQL subscriptions
        productCreatedSink.tryEmitNext(savedProduct);
        
        return savedProduct;
    }
    
    @Transactional
    public Product updateProduct(Long id, String name, String description, BigDecimal price, Integer stockQuantity, Long categoryId) {
        // Find existing product
        Product product = findProductById(id);
        
        // Find category if provided
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        }
        
        // Update product fields
        if (name != null) product.setName(name);
        if (description != null) product.setDescription(description);
        if (price != null) product.setPrice(price);
        if (stockQuantity != null) product.setStockQuantity(stockQuantity);
        if (category != null) product.setCategory(category);
        
        Product updatedProduct = productRepository.save(product);
        
        // Send Kafka event if enabled
        if (kafkaEnabled) {
            ProductEvent event = new ProductEvent("UPDATED", updatedProduct.getId());
            kafkaTemplate.send(PRODUCT_TOPIC, event);
            log.info("Sent product updated event for product id: {}", updatedProduct.getId());
        } else {
            log.info("Kafka disabled: Skipping product updated event for product id: {}", updatedProduct.getId());
        }
        
        // Emit for GraphQL subscriptions
        productUpdatedSink.tryEmitNext(updatedProduct);
        
        return updatedProduct;
    }
    
    @Transactional
    public boolean deleteProduct(Long id) {
        // Check if product exists
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        
        productRepository.deleteById(id);
        
        // Send Kafka event if enabled
        if (kafkaEnabled) {
            ProductEvent event = new ProductEvent("DELETED", id);
            kafkaTemplate.send(PRODUCT_TOPIC, event);
            log.info("Sent product deleted event for product id: {}", id);
        } else {
            log.info("Kafka disabled: Skipping product deleted event for product id: {}", id);
        }
        
        return true;
    }
    
    // Subscription methods
    public Flux<Product> getProductCreatedPublisher() {
        return productCreatedSink.asFlux();
    }
    
    public Flux<Product> getProductUpdatedPublisher() {
        return productUpdatedSink.asFlux();
    }
}
