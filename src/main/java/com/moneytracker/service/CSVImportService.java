package com.moneytracker.service;

import com.moneytracker.model.*;
import com.moneytracker.repository.CategoryRepository;
import com.moneytracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CSVImportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<Transaction> importTransactions(MultipartFile file, User user) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                try {
                    Transaction transaction = parseCSVRecord(record, user, file.getOriginalFilename());
                    transactions.add(transaction);
                } catch (Exception e) {
                    // Log error but continue with other records
                    System.err.println("Error parsing CSV record: " + e.getMessage());
                }
            }
        }

        // Save all transactions
        List<Transaction> saved = transactionRepository.saveAll(transactions);

        // Create notification
        notificationService.createNotification(
                user,
                NotificationType.SYSTEM,
                "CSV Import Complete",
                String.format("Successfully imported %d transactions from %s",
                        saved.size(), file.getOriginalFilename())
        );

        return saved;
    }

    private Transaction parseCSVRecord(CSVRecord record, User user, String fileName) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);

        // Expected CSV format: Date,Description,Amount,Category,PaymentMethod,Notes
        transaction.setTransactionDate(LocalDate.parse(
                record.get("Date"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ));
        transaction.setDescription(record.get("Description"));
        transaction.setAmount(new BigDecimal(record.get("Amount")));

        // ✅ SECURE - Find category for THIS USER only
        String categoryName = record.get("Category");
        Category category = categoryRepository.findByNameAndUserId(categoryName, user.getId())
                .orElseGet(() -> createDefaultCategory(categoryName, user));  // ✅ Pass user
        transaction.setCategory(category);

        // Parse payment method
        try {
            transaction.setPaymentMethod(PaymentMethod.valueOf(record.get("PaymentMethod")));
        } catch (Exception e) {
            transaction.setPaymentMethod(PaymentMethod.CASH);
        }

        if (record.isMapped("Notes")) {
            transaction.setNotes(record.get("Notes"));
        }

        transaction.setSource(TransactionSource.CSV_IMPORT);
        transaction.setSourceReference(fileName);

        return transaction;
    }

    /**
     * ✅ SECURE - Creates category for specific user
     */
    private Category createDefaultCategory(String categoryName, User user) {
        Category category = new Category();
        category.setUser(user);  // ✅ CRITICAL - Set the owner!
        category.setName(categoryName);
        category.setDescription("Auto-created from CSV import");
        category.setColorCode("#667eea");
        return categoryRepository.save(category);
    }

    public String generateCSVTemplate() {
        return "Date,Description,Amount,Category,PaymentMethod,Notes\n" +
                "2026-01-31,Sample Transaction,1000.00,Food & Dining,UPI,Sample notes\n";
    }
}
