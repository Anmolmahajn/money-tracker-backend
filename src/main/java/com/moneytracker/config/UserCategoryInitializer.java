package com.moneytracker.config;

import com.moneytracker.model.Category;
import com.moneytracker.model.User;
import com.moneytracker.repository.CategoryRepository;
import com.moneytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ SECURE - Creates default categories for each new user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserCategoryInitializer {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * This method is called by UserService when a new user registers
     * Called from AuthenticationService.register()
     */
    public void createDefaultCategoriesForUser(User user) {
        // Check if user already has categories
        long categoryCount = categoryRepository.findByUserId(user.getId()).size();

        if (categoryCount == 0) {
            log.info("Creating default categories for user: {}", user.getUsername());

            List<Category> defaultCategories = Arrays.asList(
                    // Expense Categories
                    createCategory(user, "Food & Dining", "Restaurants, takeout, dining", "utensils", "#FF6B6B"),
                    createCategory(user, "Shopping", "Retail purchases, clothing", "shopping-bag", "#4ECDC4"),
                    createCategory(user, "Transportation", "Uber, Ola, fuel, transit", "car", "#45B7D1"),
                    createCategory(user, "Entertainment", "Movies, games, subscriptions", "film", "#FFA07A"),
                    createCategory(user, "Bills & Utilities", "Rent, electricity, water", "file-text", "#98D8C8"),
                    createCategory(user, "Healthcare", "Medical expenses, pharmacy", "heart", "#F7DC6F"),
                    createCategory(user, "Education", "Courses, books, tuition", "book", "#BB8FCE"),
                    createCategory(user, "Groceries", "Supermarket, household items", "shopping-cart", "#FF8C42"),
                    createCategory(user, "Travel", "Flights, hotels, vacation", "plane", "#85C1E2"),
                    createCategory(user, "Fitness", "Gym, sports, wellness", "activity", "#A8E6CF"),
                    createCategory(user, "Gifts & Donations", "Presents, charity", "gift", "#FFD3B6"),
                    createCategory(user, "Personal Care", "Salon, spa, grooming", "scissors", "#FFAAA5"),

                    // Income Categories
                    createCategory(user, "Salary", "Monthly income, salary", "dollar-sign", "#52C41A"),
                    createCategory(user, "Freelance", "Freelance work, side projects", "briefcase", "#1890FF"),
                    createCategory(user, "Investments", "Stocks, mutual funds, returns", "trending-up", "#13C2C2"),
                    createCategory(user, "Business", "Business income, profits", "building", "#2F54EB"),

                    // Other
                    createCategory(user, "Other", "Miscellaneous expenses", "more-horizontal", "#8C8C8C")
            );

            categoryRepository.saveAll(defaultCategories);
            log.info("✅ Created {} default categories for user: {}", defaultCategories.size(), user.getUsername());
        }
    }

    private Category createCategory(User user, String name, String description, String iconName, String colorCode) {
        Category category = new Category();
        category.setUser(user);  // ✅ Set the owner
        category.setName(name);
        category.setDescription(description);
        category.setIconName(iconName);
        category.setColorCode(colorCode);
        return category;
    }
}