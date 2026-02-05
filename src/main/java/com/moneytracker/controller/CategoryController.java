package com.moneytracker.controller;

import com.moneytracker.dto.CategoryDTO;
import com.moneytracker.service.CategoryService;
import com.moneytracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    /**
     * ✅ SECURE - Get only current user's categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<CategoryDTO> categories = categoryService.getUserCategories(userId);
        return ResponseEntity.ok(categories);
    }

    /**
     * ✅ SECURE - Get category only if belongs to current user
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        CategoryDTO category = categoryService.getCategoryById(id, userId);
        return ResponseEntity.ok(category);
    }

    /**
     * ✅ SECURE - Create category for current user
     */
    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @Valid @RequestBody CategoryDTO categoryDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        CategoryDTO createdCategory = categoryService.createCategory(categoryDTO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * ✅ SECURE - Update only if category belongs to current user
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO, userId);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * ✅ SECURE - Delete only if category belongs to current user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.noContent().build();
    }
}
