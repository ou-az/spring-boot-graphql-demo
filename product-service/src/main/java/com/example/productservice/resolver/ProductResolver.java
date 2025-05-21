package com.example.productservice.resolver;

import com.example.productservice.model.Product;
import com.example.productservice.service.ProductService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProductResolver {

    private final ProductService productService;

    @QueryMapping
    public List<Product> products() {
        return productService.findAllProducts();
    }

    @QueryMapping
    public Product product(@Argument("id") Long id) {
        return productService.findProductById(id);
    }

    @QueryMapping
    public List<Product> productsByCategory(@Argument("categoryId") Long categoryId) {
        return productService.findProductsByCategoryId(categoryId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@Argument("input") Map<String, Object> input) {
        String name = (String) input.get("name");
        String description = (String) input.get("description");
        BigDecimal price = new BigDecimal(input.get("price").toString());
        Integer stockQuantity = (Integer) input.get("stockQuantity");
        Long categoryId = Long.valueOf(input.get("categoryId").toString());

        return productService.createProduct(name, description, price, stockQuantity, categoryId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(@Argument("id") Long id, @Argument("input") Map<String, Object> input) {
        String name = input.containsKey("name") ? (String) input.get("name") : null;
        String description = input.containsKey("description") ? (String) input.get("description") : null;
        BigDecimal price = input.containsKey("price") ? new BigDecimal(input.get("price").toString()) : null;
        Integer stockQuantity = input.containsKey("stockQuantity") ? (Integer) input.get("stockQuantity") : null;
        Long categoryId = input.containsKey("categoryId") ? Long.valueOf(input.get("categoryId").toString()) : null;

        return productService.updateProduct(id, name, description, price, stockQuantity, categoryId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteProduct(@Argument("id") Long id) {
        return productService.deleteProduct(id);
    }

    @SubscriptionMapping
    public Flux<Product> productCreated() {
        return productService.getProductCreatedPublisher();
    }

    @SubscriptionMapping
    public Flux<Product> productUpdated() {
        return productService.getProductUpdatedPublisher();
    }
}
