package com.moneta.config;

import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        seedDefaultCategories();
    }

    private void seedDefaultCategories() {
        // Required Default categories from assignment PDF:
        // INCOME: Salary
        // EXPENSE: Food, Rent, Transportation, Entertainment, Healthcare, Utilities
        seedCategoryIfMissing("Salary", CategoryType.INCOME);
        seedCategoryIfMissing("Food", CategoryType.EXPENSE);
        seedCategoryIfMissing("Rent", CategoryType.EXPENSE);
        seedCategoryIfMissing("Transportation", CategoryType.EXPENSE);
        seedCategoryIfMissing("Entertainment", CategoryType.EXPENSE);
        seedCategoryIfMissing("Healthcare", CategoryType.EXPENSE);
        seedCategoryIfMissing("Utilities", CategoryType.EXPENSE);
    }

    private void seedCategoryIfMissing(String name, CategoryType type) {
        if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            Category category = new Category(name, type, false, null);
            categoryRepository.save(category);
        }
    }
}
