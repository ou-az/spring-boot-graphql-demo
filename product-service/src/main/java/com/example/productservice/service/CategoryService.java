package com.example.productservice.service;

import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.model.Category;
import com.example.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
    
    @Transactional
    public Category createCategory(String name, String description) {
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Category category = findCategoryById(id);
        
        if (name != null) category.setName(name);
        if (description != null) category.setDescription(description);
        
        return categoryRepository.save(category);
    }
    
    @Transactional
    public boolean deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        
        // Note: This will only work if there are no products associated with this category
        // due to the foreign key constraint
        try {
            categoryRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete category with id: {}", id, e);
            return false;
        }
    }
}
