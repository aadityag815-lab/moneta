package com.moneta.service;

import com.moneta.dto.CategoryListResponse;
import com.moneta.dto.CategoryRequest;
import com.moneta.dto.CategoryResponse;
import com.moneta.dto.MessageResponse;
import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.entity.User;
import com.moneta.exception.BadRequestException;
import com.moneta.exception.ConflictException;
import com.moneta.exception.ForbiddenException;
import com.moneta.exception.ResourceNotFoundException;
import com.moneta.repository.CategoryRepository;
import com.moneta.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "pass", "John Doe", "1234");
        testUser.setId(1L);
    }

    @Test
    void getAllCategories_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        Category defaultCategory = new Category("Salary", CategoryType.INCOME, false, null);
        Category customCategory = new Category("Bonus", CategoryType.INCOME, true, testUser);

        when(categoryRepository.findAllAccessibleCategories(testUser.getId()))
                .thenReturn(Arrays.asList(defaultCategory, customCategory));

        CategoryListResponse response = categoryService.getAllCategories();

        assertNotNull(response);
        assertEquals(2, response.getCategories().size());
        assertEquals("Salary", response.getCategories().get(0).getName());
        assertFalse(response.getCategories().get(0).isCustom());
        assertEquals("Bonus", response.getCategories().get(1).getName());
        assertTrue(response.getCategories().get(1).isCustom());
    }

    @Test
    void createCustomCategory_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);

        CategoryRequest request = new CategoryRequest("Freelance", "INCOME");
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(false);

        Category saved = new Category("Freelance", CategoryType.INCOME, true, testUser);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCustomCategory(request);

        assertNotNull(response);
        assertEquals("Freelance", response.getName());
        assertEquals("INCOME", response.getType());
        assertTrue(response.isCustom());
    }

    @Test
    void createCustomCategory_DuplicateDefaultName_ThrowsConflict() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        CategoryRequest request = new CategoryRequest("Salary", "INCOME");
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(request));
    }

    @Test
    void createCustomCategory_DuplicateCustomName_ThrowsConflict() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        CategoryRequest request = new CategoryRequest("Freelance", "INCOME");
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCustomCategory(request));
    }

    @Test
    void createCustomCategory_InvalidType_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        CategoryRequest request = new CategoryRequest("Freelance", "INVALID_TYPE");

        assertThrows(BadRequestException.class, () -> categoryService.createCustomCategory(request));
    }

    @Test
    void deleteCategory_DefaultCategory_ThrowsForbidden() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        Category defaultCategory = new Category("Salary", CategoryType.INCOME, false, null);
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(Optional.of(defaultCategory));

        assertThrows(ForbiddenException.class, () -> categoryService.deleteCategory("Salary"));
    }

    @Test
    void deleteCategory_NonExistent_ThrowsNotFound() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Unknown")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Unknown", testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory("Unknown"));
    }

    @Test
    void deleteCategory_ReferencedByTransactions_ThrowsBadRequest() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Bonus")).thenReturn(Optional.empty());

        Category customCategory = new Category("Bonus", CategoryType.INCOME, true, testUser);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Bonus", testUser)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByUserAndCategoryEntity(testUser, customCategory)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory("Bonus"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategory_Success() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(testUser);
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Bonus")).thenReturn(Optional.empty());

        Category customCategory = new Category("Bonus", CategoryType.INCOME, true, testUser);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Bonus", testUser)).thenReturn(Optional.of(customCategory));
        when(transactionRepository.existsByUserAndCategoryEntity(testUser, customCategory)).thenReturn(false);
        when(transactionRepository.existsByUserAndCategoryIgnoreCase(testUser, "Bonus")).thenReturn(false);

        MessageResponse response = categoryService.deleteCategory("Bonus");

        assertEquals("Category deleted successfully", response.getMessage());
        verify(categoryRepository).delete(customCategory);
    }
}
