package com.moneta.controller;

import com.moneta.dto.CategoryListResponse;
import com.moneta.dto.CategoryRequest;
import com.moneta.dto.CategoryResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<CategoryListResponse> getAllCategories() {
        CategoryListResponse response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCustomCategory(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable("name") String name) {
        MessageResponse response = categoryService.deleteCategory(name);
        return ResponseEntity.ok(response);
    }
}
