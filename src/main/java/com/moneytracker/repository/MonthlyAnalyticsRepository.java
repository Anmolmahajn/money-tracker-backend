package com.moneytracker.repository;

import com.moneytracker.model.MonthlyAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyAnalyticsRepository extends JpaRepository<MonthlyAnalytics, Long> {
    
    Optional<MonthlyAnalytics> findByUserIdAndYearMonth(Long userId, String yearMonth);
    
    List<MonthlyAnalytics> findByUserIdOrderByYearMonthDesc(Long userId);
    
    List<MonthlyAnalytics> findTop12ByUserIdOrderByYearMonthDesc(Long userId);
}
