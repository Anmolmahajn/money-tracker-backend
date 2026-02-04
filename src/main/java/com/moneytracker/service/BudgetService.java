package com.moneytracker.service;

import com.moneytracker.dto.BudgetDTO;
import com.moneytracker.exception.ResourceNotFoundException;
import com.moneytracker.model.*;
import com.moneytracker.repository.BudgetRepository;
import com.moneytracker.repository.TransactionRepository;
import com.moneytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<BudgetDTO> getUserBudgets(Long userId) {
        return budgetRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetDTO createBudget(Long userId, BudgetDTO budgetDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Budget budget = convertToEntity(budgetDTO);
        budget.setUser(user);
        budget.setIsActive(true);

        Budget saved = budgetRepository.save(budget);
        return convertToDTO(saved);
    }

    @Transactional
    public void checkBudgetAlerts(Long userId) {
        List<Budget> budgets = budgetRepository.findByUserIdAndIsActiveTrue(userId);
        LocalDate now = LocalDate.now();

        for (Budget budget : budgets) {
            if (budget.getStartDate().isAfter(now) || budget.getEndDate().isBefore(now)) {
                continue;
            }

            BigDecimal spent = calculateSpentAmount(budget);
            double percentageUsed = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            if (percentageUsed >= budget.getAlertThreshold()) {
                sendBudgetAlert(budget, percentageUsed, spent);
            }
        }
    }

    // Scheduled task to check all budgets daily
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void checkAllBudgetsDaily() {
        List<Budget> activeBudgets = budgetRepository.findAllActiveBudgets(LocalDate.now());

        for (Budget budget : activeBudgets) {
            BigDecimal spent = calculateSpentAmount(budget);
            double percentageUsed = spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

            if (percentageUsed >= budget.getAlertThreshold() && shouldSendAlert(budget)) {
                sendBudgetAlert(budget, percentageUsed, spent);
            }
        }
    }

    private BigDecimal calculateSpentAmount(Budget budget) {
        if (budget.getCategory() == null) {
            // Overall budget
            return transactionRepository.getUserTotalSpendingInDateRange(
                    budget.getUser().getId(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );
        } else {
            // Category-specific budget
            return transactionRepository.findByUserIdAndCategoryId(
                            budget.getUser().getId(),
                            budget.getCategory().getId()
                    ).stream()
                    .filter(t -> !t.getTransactionDate().isBefore(budget.getStartDate()) &&
                            !t.getTransactionDate().isAfter(budget.getEndDate()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    private boolean shouldSendAlert(Budget budget) {
        if (budget.getLastAlertSent() == null) {
            return true;
        }
        // Don't send more than one alert per day
        return budget.getLastAlertSent().isBefore(LocalDateTime.now().minusDays(1));
    }

    private void sendBudgetAlert(Budget budget, double percentageUsed, BigDecimal spent) {
        User user = budget.getUser();
        String budgetName = budget.getCategory() != null ?
                budget.getCategory().getName() : "Overall";

        // Create notification
        NotificationType type = percentageUsed >= 100 ?
                NotificationType.BUDGET_EXCEEDED :
                NotificationType.BUDGET_ALERT;

        String title = String.format("Budget Alert: %s", budgetName);
        String message = String.format(
                "You've used %.1f%% of your %s budget. Spent: ₹%.2f / ₹%.2f",
                percentageUsed, budgetName, spent, budget.getAmount()
        );

        notificationService.createNotification(user, type, title, message);

        // Update last alert sent time
        budget.setLastAlertSent(LocalDateTime.now());
        budgetRepository.save(budget);
    }

    private BudgetDTO convertToDTO(Budget budget) {
        BudgetDTO dto = new BudgetDTO();
        dto.setId(budget.getId());
        dto.setAmount(budget.getAmount());
        dto.setPeriodType(budget.getPeriodType());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setAlertThreshold(budget.getAlertThreshold());
        dto.setIsActive(budget.getIsActive());
        if (budget.getCategory() != null) {
            dto.setCategoryId(budget.getCategory().getId());
            dto.setCategoryName(budget.getCategory().getName());
        }

        // Calculate current spending
        BigDecimal spent = calculateSpentAmount(budget);
        dto.setCurrentSpending(spent);
        dto.setPercentageUsed(
                spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
        );

        return dto;
    }

    private Budget convertToEntity(BudgetDTO dto) {
        Budget budget = new Budget();
        budget.setAmount(dto.getAmount());
        budget.setPeriodType(dto.getPeriodType());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setAlertThreshold(dto.getAlertThreshold() != null ? dto.getAlertThreshold() : 80);
        return budget;
    }
}
