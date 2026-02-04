package com.moneytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAnalyticsDTO {
    private String yearMonth;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Integer transactionCount;
    private String topCategory;
    private BigDecimal topCategoryAmount;
    private BigDecimal avgTransactionAmount;
    private Map<String, BigDecimal> categoryBreakdown;
    private Map<String, BigDecimal> paymentMethodBreakdown;
}
