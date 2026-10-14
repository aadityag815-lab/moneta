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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository,
                           AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public CategoryListResponse getAllCategories() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<Category> categories = categoryRepository.findAllAccessibleCategories(currentUser.getId());

        List<CategoryResponse> responses = new ArrayList<>();
        for (Category category : categories) {
            responses.add(new CategoryResponse(category.getName(), category.getType().name(), category.isCustom()));
        }

        return new CategoryListResponse(responses);
    }

    @Transactional
    public CategoryResponse createCustomCategory(CategoryRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Category name cannot be empty");
        }

        CategoryType type;
        try {
            type = CategoryType.valueOf(request.getType().trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid category type. Must be INCOME or EXPENSE");
        }

        String trimmedName = request.getName().trim();

        // Check if name conflicts with default categories
        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(trimmedName)) {
            throw new ConflictException("Category already exists with name: " + trimmedName);
        }

        // Check if user already has a custom category with this name
        if (categoryRepository.existsByNameIgnoreCaseAndUser(trimmedName, currentUser)) {
            throw new ConflictException("Custom category already exists with name: " + trimmedName);
        }

        Category customCategory = new Category(trimmedName, type, true, currentUser);
        Category saved = categoryRepository.save(customCategory);

        return new CategoryResponse(saved.getName(), saved.getType().name(), saved.isCustom());
    }

    @Transactional
    public MessageResponse deleteCategory(String name) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        if (name == null || name.trim().isEmpty()) {
            throw new BadRequestException("Category name is required");
        }

        String trimmedName = name.trim();

        // Check if it's a default category
        Optional<Category> defaultCategory = categoryRepository.findByNameIgnoreCaseAndUserIsNull(trimmedName);
        if (defaultCategory.isPresent()) {
            throw new ForbiddenException("Default categories cannot be deleted or modified");
        }

        // Check user's custom category
        Category customCategory = categoryRepository.findByNameIgnoreCaseAndUser(trimmedName, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + trimmedName));

        // Check if category is currently referenced by transactions
        boolean isReferenced = transactionRepository.existsByUserAndCategoryEntity(currentUser, customCategory)
                || transactionRepository.existsByUserAndCategoryIgnoreCase(currentUser, trimmedName);

        if (isReferenced) {
            throw new BadRequestException("Category cannot be deleted because it is currently referenced by transactions");
        }

        categoryRepository.delete(customCategory);
        return new MessageResponse("Category deleted successfully");
    }

    @Transactional(readOnly = true)
    public Category findAccessibleCategoryByName(String name, User user) {
        return categoryRepository.findAccessibleCategoryByName(name.trim(), user.getId())
                .orElseThrow(() -> new BadRequestException("Category '" + name + "' is not a valid accessible category"));
    }
}
