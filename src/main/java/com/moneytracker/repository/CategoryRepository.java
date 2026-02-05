package com.moneytracker.repository;

import com.moneytracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // ✅ SECURE - Filter by user
    List<Category> findByUserId(Long userId);
    Optional<Category> findByNameAndUserId(String name, Long userId);
    boolean existsByNameAndUserId(String name, Long userId);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
