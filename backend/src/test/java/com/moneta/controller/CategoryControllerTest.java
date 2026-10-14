package com.moneta.controller;

import com.moneta.dto.CategoryListResponse;
import com.moneta.dto.CategoryRequest;
import com.moneta.dto.CategoryResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void getAllCategories() {
        when(categoryService.getAllCategories()).thenReturn(new CategoryListResponse(Collections.emptyList()));

        ResponseEntity<CategoryListResponse> res = categoryController.getAllCategories();
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void createCategory() {
        CategoryRequest req = new CategoryRequest("Freelance", "INCOME");
        when(categoryService.createCustomCategory(req)).thenReturn(new CategoryResponse("Freelance", "INCOME", true));

        ResponseEntity<CategoryResponse> res = categoryController.createCategory(req);
        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertEquals("Freelance", res.getBody().getName());
    }

    @Test
    void deleteCategory() {
        when(categoryService.deleteCategory("Freelance")).thenReturn(new MessageResponse("Category deleted successfully"));

        ResponseEntity<MessageResponse> res = categoryController.deleteCategory("Freelance");
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Category deleted successfully", res.getBody().getMessage());
    }
}
