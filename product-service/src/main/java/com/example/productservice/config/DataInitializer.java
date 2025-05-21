package com.example.productservice.config;

import com.example.productservice.model.Category;
import com.example.productservice.model.Product;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (categoryRepository.count() > 0) {
                log.info("Database already initialized, skipping initialization");
                return;
            }

            log.info("Initializing database with sample data");

            // Create categories
            Category electronics = categoryRepository.save(Category.builder()
                    .name("Electronics")
                    .description("Electronic equipment and devices")
                    .build());

            Category clothing = categoryRepository.save(Category.builder()
                    .name("Clothing")
                    .description("Apparel and fashion items")
                    .build());

            Category books = categoryRepository.save(Category.builder()
                    .name("Books")
                    .description("Books, ebooks, and audiobooks")
                    .build());

            // Create products
            List<Product> products = List.of(
                    Product.builder()
                            .name("Smartphone X")
                            .description("The latest flagship smartphone with cutting-edge features")
                            .price(new BigDecimal("999.99"))
                            .stockQuantity(50)
                            .category(electronics)
                            .build(),
                    Product.builder()
                            .name("Laptop Pro")
                            .description("High-performance laptop for professionals")
                            .price(new BigDecimal("1499.99"))
                            .stockQuantity(25)
                            .category(electronics)
                            .build(),
                    Product.builder()
                            .name("Wireless Headphones")
                            .description("Premium wireless noise-cancelling headphones")
                            .price(new BigDecimal("249.99"))
                            .stockQuantity(100)
                            .category(electronics)
                            .build(),
                    Product.builder()
                            .name("Cotton T-Shirt")
                            .description("Comfortable 100% cotton t-shirt")
                            .price(new BigDecimal("19.99"))
                            .stockQuantity(200)
                            .category(clothing)
                            .build(),
                    Product.builder()
                            .name("Denim Jeans")
                            .description("Classic denim jeans with modern fit")
                            .price(new BigDecimal("59.99"))
                            .stockQuantity(150)
                            .category(clothing)
                            .build(),
                    Product.builder()
                            .name("JavaScript: The Good Parts")
                            .description("A book focusing on the good features of JavaScript")
                            .price(new BigDecimal("29.99"))
                            .stockQuantity(75)
                            .category(books)
                            .build(),
                    Product.builder()
                            .name("Spring Boot in Action")
                            .description("Learn Spring Boot development by example")
                            .price(new BigDecimal("39.99"))
                            .stockQuantity(60)
                            .category(books)
                            .build(),
                    Product.builder()
                            .name("Effective Java")
                            .description("Best practices for Java programming")
                            .price(new BigDecimal("44.99"))
                            .stockQuantity(40)
                            .category(books)
                            .build()
            );

            productRepository.saveAll(products);

            log.info("Sample data initialized with {} categories and {} products",
                    categoryRepository.count(), productRepository.count());
        };
    }
}
