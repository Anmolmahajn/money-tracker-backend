package com.moneytracker.service;

import com.moneytracker.dto.CategoryDTO;
import com.moneytracker.exception.ResourceNotFoundException;
import com.moneytracker.model.Category;
import com.moneytracker.model.User;
import com.moneytracker.repository.CategoryRepository;
import com.moneytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * ✅ SECURE - Get only current user's categories
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getUserCategories(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ SECURE - Get category only if it belongs to user
     */
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));
        return convertToDTO(category);
    }

    /**
     * ✅ SECURE - Create category for specific user
     */
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO, Long userId) {
        // Check if category name already exists for this user
        if (categoryRepository.existsByNameAndUserId(categoryDTO.getName(), userId)) {
            throw new IllegalArgumentException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Category category = convertToEntity(categoryDTO);
        category.setUser(user);  // ✅ Set the owner

        Category savedCategory = categoryRepository.save(category);
        return convertToDTO(savedCategory);
    }

    /**
     * ✅ SECURE - Update only if category belongs to user
     */
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO, Long userId) {
        Category existingCategory = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));

        // Check if new name conflicts with another category
        if (!existingCategory.getName().equals(categoryDTO.getName()) &&
                categoryRepository.existsByNameAndUserId(categoryDTO.getName(), userId)) {
            throw new IllegalArgumentException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());
        existingCategory.setIconName(categoryDTO.getIconName());
        existingCategory.setColorCode(categoryDTO.getColorCode());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return convertToDTO(updatedCategory);
    }

    /**
     * ✅ SECURE - Delete only if category belongs to user
     */
    @Transactional
    public void deleteCategory(Long id, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));
        categoryRepository.delete(category);
    }

    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIconName(category.getIconName());
        dto.setColorCode(category.getColorCode());
        return dto;
    }

    private Category convertToEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setIconName(dto.getIconName());
        category.setColorCode(dto.getColorCode());
        return category;
    }
}
