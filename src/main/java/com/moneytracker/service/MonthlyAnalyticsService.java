package com.moneytracker.service;

import com.moneytracker.dto.MonthlyAnalyticsDTO;
import com.moneytracker.model.MonthlyAnalytics;
import com.moneytracker.model.Transaction;
import com.moneytracker.model.User;
import com.moneytracker.repository.MonthlyAnalyticsRepository;
import com.moneytracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyAnalyticsService {

    private final MonthlyAnalyticsRepository analyticsRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    @Transactional
    public MonthlyAnalyticsDTO generateMonthlyAnalytics(User user, YearMonth yearMonth) {
        String yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        MonthlyAnalytics analytics = analyticsRepository
                .findByUserIdAndYearMonth(user.getId(), yearMonthStr)
                .orElseGet(() -> {
                    MonthlyAnalytics newAnalytics = new MonthlyAnalytics();
                    newAnalytics.setUser(user);
                    newAnalytics.setYearMonth(yearMonthStr);
                    return newAnalytics;
                });

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findUserTransactionsInDateRange(user.getId(), startDate, endDate);

        // Calculate metrics
        BigDecimal totalExpenses = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        analytics.setTotalExpenses(totalExpenses);
        analytics.setTransactionCount(transactions.size());
        analytics.setAvgTransactionAmount(
                transactions.isEmpty() ? BigDecimal.ZERO :
                        totalExpenses.divide(BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP)
        );

        // Category breakdown
        Map<String, BigDecimal> categoryMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // Find top category
        Optional<Map.Entry<String, BigDecimal>> topEntry = categoryMap.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        topEntry.ifPresent(entry -> {
            analytics.setTopCategory(entry.getKey());
            analytics.setTopCategoryAmount(entry.getValue());
        });

        analytics.setCategoryBreakdown(convertMapToJson(categoryMap));

        // Payment method breakdown
        Map<String, BigDecimal> paymentMap = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPaymentMethod().getDisplayName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
        analytics.setPaymentMethodBreakdown(convertMapToJson(paymentMap));

        MonthlyAnalytics saved = analyticsRepository.save(analytics);
        return convertToDTO(saved, categoryMap, paymentMap);
    }

    @Scheduled(cron = "0 0 2 1 * *") // First day of month at 2 AM
    public void generateMonthlyReportsForAllUsers() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        // This would iterate through all users - simplified for example
        // In production, you'd fetch all users and generate reports
    }

    public List<MonthlyAnalyticsDTO> getUserMonthlyAnalytics(Long userId, int months) {
        return analyticsRepository.findTop12ByUserIdOrderByYearMonthDesc(userId).stream()
                .limit(months)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private String convertMapToJson(Map<String, BigDecimal> map) {
        // Simple JSON conversion - in production use Jackson
        return map.entrySet().stream()
                .map(e -> String.format("\\\"%s\\\":%.2f", e.getKey(), e.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private MonthlyAnalyticsDTO convertToDTO(MonthlyAnalytics analytics) {
        return convertToDTO(analytics, null, null);
    }

    private MonthlyAnalyticsDTO convertToDTO(MonthlyAnalytics analytics,
                                             Map<String, BigDecimal> categoryMap,
                                             Map<String, BigDecimal> paymentMap) {
        MonthlyAnalyticsDTO dto = new MonthlyAnalyticsDTO();
        dto.setYearMonth(analytics.getYearMonth());
        dto.setTotalExpenses(analytics.getTotalExpenses());
        dto.setTransactionCount(analytics.getTransactionCount());
        dto.setTopCategory(analytics.getTopCategory());
        dto.setTopCategoryAmount(analytics.getTopCategoryAmount());
        dto.setAvgTransactionAmount(analytics.getAvgTransactionAmount());
        dto.setCategoryBreakdown(categoryMap);
        dto.setPaymentMethodBreakdown(paymentMap);
        return dto;
    }
}
