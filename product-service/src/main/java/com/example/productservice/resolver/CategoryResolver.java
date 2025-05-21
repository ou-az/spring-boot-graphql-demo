package com.example.productservice.resolver;

import com.example.productservice.model.Category;
import com.example.productservice.model.Product;
import com.example.productservice.service.CategoryService;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CategoryResolver {

    private final CategoryService categoryService;
    private final ProductService productService;

    @QueryMapping
    public List<Category> categories() {
        return categoryService.findAllCategories();
    }

    @QueryMapping
    public Category category(@Argument("id") Long id) {
        return categoryService.findCategoryById(id);
    }

    @SchemaMapping(typeName = "Category", field = "products")
    public List<Product> getProducts(Category category) {
        return productService.findProductsByCategoryId(category.getId());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Category createCategory(@Argument("input") Map<String, Object> input) {
        String name = (String) input.get("name");
        String description = (String) input.get("description");

        return categoryService.createCategory(name, description);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Category updateCategory(@Argument("id") Long id, @Argument("input") Map<String, Object> input) {
        String name = input.containsKey("name") ? (String) input.get("name") : null;
        String description = input.containsKey("description") ? (String) input.get("description") : null;

        return categoryService.updateCategory(id, name, description);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteCategory(@Argument("id") Long id) {
        return categoryService.deleteCategory(id);
    }
}
