package com.moneytracker.repository;

import com.moneytracker.model.Budget;
import com.moneytracker.model.BudgetPeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    
    List<Budget> findByUserIdAndIsActiveTrue(Long userId);
    
    List<Budget> findByUserIdAndCategoryIdAndIsActiveTrue(Long userId, Long categoryId);
    
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.isActive = true " +
           "AND b.periodType = :periodType AND :currentDate BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsForPeriod(@Param("userId") Long userId,
                                           @Param("periodType") BudgetPeriodType periodType,
                                           @Param("currentDate") LocalDate currentDate);
    
    @Query("SELECT b FROM Budget b WHERE b.isActive = true " +
           "AND :currentDate BETWEEN b.startDate AND b.endDate")
    List<Budget> findAllActiveBudgets(@Param("currentDate") LocalDate currentDate);
}
