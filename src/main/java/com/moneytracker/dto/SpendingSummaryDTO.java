package com.moneytracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpendingSummaryDTO {
    private BigDecimal totalSpending;
    private Map<String, BigDecimal> categoryBreakdown;
    private Map<String, BigDecimal> paymentMethodBreakdown;
    private Integer transactionCount;
}
