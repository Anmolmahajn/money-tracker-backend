package com.moneytracker.dto;

import com.moneytracker.model.BudgetPeriodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDTO {
    private Long id;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private BudgetPeriodType periodType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private Integer alertThreshold;
    private Boolean isActive;

    private Long categoryId;
    private String categoryName;

    // Calculated fields
    private BigDecimal currentSpending;
    private Double percentageUsed;
}