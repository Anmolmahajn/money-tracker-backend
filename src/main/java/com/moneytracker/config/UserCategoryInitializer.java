package com.moneytracker.config;

import com.moneytracker.model.Category;
import com.moneytracker.model.User;
import com.moneytracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ SECURE - Creates default categories for each new user
 * Each user gets their own set of categories
 * Users can have duplicate category names (isolated per user)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCategoryInitializer {

    private final CategoryRepository categoryRepository;

    /**
     * This method is called by AuthenticationService when a new user registers
     * Creates a complete set of default categories for the user
     */
    public void createDefaultCategoriesForUser(User user) {
        // Check if user already has categories
        long categoryCount = categoryRepository.findByUserId(user.getId()).size();

        if (categoryCount == 0) {
            log.info("🔄 Creating default categories for user: {}", user.getUsername());

            List<Category> defaultCategories = Arrays.asList(
                    // Food & Dining Categories
                    createCategory(user, "Food", "General food expenses", "utensils", "#FF6B6B"),
                    createCategory(user, "Uber", "Uber Eats orders", "shopping-bag", "#4ECDC4"),
                    createCategory(user, "Dining", "Restaurant dining", "utensils", "#FF8C42"),

                    // Shopping Categories
                    createCategory(user, "Retail purchases", "General retail shopping", "shopping-bag", "#4ECDC4"),
                    createCategory(user, "Shopping", "Shopping expenses", "shopping-bag", "#A8E6CF"),

                    // Transportation Categories
                    createCategory(user, "Uber, fuel, transit", "Uber rides and fuel", "car", "#45B7D1"),
                    createCategory(user, "Transportation", "General transportation", "car", "#85C1E2"),
                    createCategory(user, "Petrol", "Fuel and petrol", "car", "#FFD93D"),
                    createCategory(user, "Car Wash", "Car maintenance", "car", "#6BCF7F"),

                    // Entertainment Categories
                    createCategory(user, "Movies, games", "Entertainment expenses", "film", "#FFA07A"),
                    createCategory(user, "Entertainment", "Movies, games, fun", "film", "#FFB6C1"),

                    // Bills & Utilities
                    createCategory(user, "Rent, electricity", "Monthly bills", "file-text", "#98D8C8"),
                    createCategory(user, "Bills & Utilities", "Utility bills", "file-text", "#B8E6D5"),

                    // Healthcare
                    createCategory(user, "Medical expenses", "Healthcare costs", "heart", "#F7DC6F"),
                    createCategory(user, "Healthcare", "Medical and health", "heart", "#FFE5B4"),

                    // Education
                    createCategory(user, "Courses, books", "Educational expenses", "book", "#BB8FCE"),
                    createCategory(user, "Education", "Learning and courses", "book", "#D4A5D4"),

                    // Groceries
                    createCategory(user, "Supermarket items", "Grocery shopping", "shopping-cart", "#FF8C42"),
                    createCategory(user, "Groceries", "Food and household items", "shopping-cart", "#FFA500"),

                    // Income Categories
                    createCategory(user, "Monthly income", "Regular salary", "dollar-sign", "#52C41A"),
                    createCategory(user, "Salary", "Monthly salary income", "dollar-sign", "#7CFC00"),
                    createCategory(user, "Side projects", "Freelance income", "briefcase", "#1890FF"),
                    createCategory(user, "Freelance", "Freelance work income", "briefcase", "#4169E1"),

                    // Other
                    createCategory(user, "Miscellaneous", "Other expenses", "more-horizontal", "#8C8C8C"),
                    createCategory(user, "Other", "Miscellaneous expenses", "more-horizontal", "#A9A9A9")
            );

            categoryRepository.saveAll(defaultCategories);
            log.info("✅ Created {} default categories for user: {}", defaultCategories.size(), user.getUsername());
        } else {
            log.info("ℹ️ User {} already has {} categories, skipping initialization",
                    user.getUsername(), categoryCount);
        }
    }

    /**
     * Helper method to create a category
     */
    private Category createCategory(User user, String name, String description, String iconName, String colorCode) {
        Category category = new Category();
        category.setUser(user);  // ✅ CRITICAL - Set the owner
        category.setName(name);
        category.setDescription(description);
        category.setIconName(iconName);
        category.setColorCode(colorCode);
        return category;
    }
}