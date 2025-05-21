package com.example.productservice.controller;

import com.example.productservice.model.Category;
import com.example.productservice.model.Product;
import com.example.productservice.service.CategoryService;
import com.example.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        List<Product> products = productService.findAllProducts();
        List<Category> categories = categoryService.findAllCategories();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && 
                authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("isAdmin", isAdmin);
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/graphql-console")
    public String graphqlConsole(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && 
                authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        model.addAttribute("isAdmin", isAdmin);
        return "graphql-console";
    }
}
