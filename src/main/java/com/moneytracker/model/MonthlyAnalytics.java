package com.moneytracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "monthly_analytics", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "year_month_val", nullable = false)
    private String yearMonth; // Format: "2026-01"
    
    @Column(name = "total_income", precision = 10, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;
    
    @Column(name = "total_expenses", precision = 10, scale = 2)
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    
    @Column(name = "transaction_count")
    private Integer transactionCount = 0;
    
    @Column(name = "top_category")
    private String topCategory;
    
    @Column(name = "top_category_amount", precision = 10, scale = 2)
    private BigDecimal topCategoryAmount = BigDecimal.ZERO;
    
    @Column(name = "avg_transaction_amount", precision = 10, scale = 2)
    private BigDecimal avgTransactionAmount = BigDecimal.ZERO;
    
    @Column(columnDefinition = "JSON")
    private String categoryBreakdown; // JSON string of category-wise spending
    
    @Column(columnDefinition = "JSON")
    private String paymentMethodBreakdown; // JSON string of payment method breakdown
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
