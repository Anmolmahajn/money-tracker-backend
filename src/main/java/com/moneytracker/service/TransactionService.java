package com.moneytracker.service;

import com.moneytracker.dto.SpendingSummaryDTO;
import com.moneytracker.dto.TransactionDTO;
import com.moneytracker.exception.ResourceNotFoundException;
import com.moneytracker.model.Category;
import com.moneytracker.model.PaymentMethod;
import com.moneytracker.model.Transaction;
import com.moneytracker.model.User;
import com.moneytracker.repository.CategoryRepository;
import com.moneytracker.repository.TransactionRepository;
import com.moneytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * ✅ SECURE - Get only current user's transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ SECURE - Get transaction only if belongs to user
     */
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Security check: verify transaction belongs to user
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found or access denied");
        }

        return convertToDTO(transaction);
    }

    /**
     * ✅ SECURE - Get user's transactions by date range
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findUserTransactionsInDateRange(userId, startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ SECURE - Get user's transactions by category
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactionsByCategory(Long userId, Long categoryId) {
        // Verify category belongs to user
        categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));

        return transactionRepository.findByUserIdAndCategoryId(userId, categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ SECURE - Get user's transactions by payment method
     */
    @Transactional(readOnly = true)
    public List<TransactionDTO> getUserTransactionsByPaymentMethod(Long userId, PaymentMethod paymentMethod) {
        return transactionRepository.findByUserIdAndPaymentMethod(userId, paymentMethod).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * ✅ SECURE - Create transaction for specific user
     */
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify category belongs to user
        Category category = categoryRepository.findByIdAndUserId(transactionDTO.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));

        Transaction transaction = convertToEntity(transactionDTO);
        transaction.setUser(user);
        transaction.setCategory(category);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return convertToDTO(savedTransaction);
    }

    /**
     * ✅ SECURE - Update only if transaction belongs to user
     */
    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionDTO transactionDTO, Long userId) {
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Security check: verify transaction belongs to user
        if (!existingTransaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found or access denied");
        }

        // Verify new category belongs to user
        Category category = categoryRepository.findByIdAndUserId(transactionDTO.getCategoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));

        existingTransaction.setDescription(transactionDTO.getDescription());
        existingTransaction.setAmount(transactionDTO.getAmount());
        existingTransaction.setTransactionDate(transactionDTO.getTransactionDate());
        existingTransaction.setPaymentMethod(transactionDTO.getPaymentMethod());
        existingTransaction.setPaymentDetails(transactionDTO.getPaymentDetails());
        existingTransaction.setCategory(category);
        existingTransaction.setNotes(transactionDTO.getNotes());
        existingTransaction.setIsRecurring(transactionDTO.getIsRecurring());

        Transaction updatedTransaction = transactionRepository.save(existingTransaction);
        return convertToDTO(updatedTransaction);
    }

    /**
     * ✅ SECURE - Delete only if transaction belongs to user
     */
    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Security check: verify transaction belongs to user
        if (!transaction.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found or access denied");
        }

        transactionRepository.delete(transaction);
    }

    /**
     * ✅ SECURE - Get spending summary for user only
     */
    @Transactional(readOnly = true)
    public SpendingSummaryDTO getUserSpendingSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        SpendingSummaryDTO summary = new SpendingSummaryDTO();

        // Total spending for user
        BigDecimal totalSpending = transactionRepository.getUserTotalSpendingInDateRange(userId, startDate, endDate);
        summary.setTotalSpending(totalSpending != null ? totalSpending : BigDecimal.ZERO);

        // Category breakdown for user
        List<Object[]> categoryData = transactionRepository.getUserSpendingByCategory(userId, startDate, endDate);
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        for (Object[] row : categoryData) {
            categoryBreakdown.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.setCategoryBreakdown(categoryBreakdown);

        // Payment method breakdown for user
        List<Object[]> paymentData = transactionRepository.getUserSpendingByPaymentMethod(userId, startDate, endDate);
        Map<String, BigDecimal> paymentBreakdown = new HashMap<>();
        for (Object[] row : paymentData) {
            PaymentMethod method = (PaymentMethod) row[0];
            paymentBreakdown.put(method.getDisplayName(), (BigDecimal) row[1]);
        }
        summary.setPaymentMethodBreakdown(paymentBreakdown);

        // Transaction count for user
        List<Transaction> transactions = transactionRepository.findUserTransactionsInDateRange(userId, startDate, endDate);
        summary.setTransactionCount(transactions.size());

        return summary;
    }

    private TransactionDTO convertToDTO(Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setDescription(transaction.getDescription());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setPaymentMethod(transaction.getPaymentMethod());
        dto.setPaymentDetails(transaction.getPaymentDetails());
        dto.setCategoryId(transaction.getCategory().getId());
        dto.setCategoryName(transaction.getCategory().getName());
        dto.setNotes(transaction.getNotes());
        dto.setIsRecurring(transaction.getIsRecurring());
        return dto;
    }

    private Transaction convertToEntity(TransactionDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setPaymentMethod(dto.getPaymentMethod());
        transaction.setPaymentDetails(dto.getPaymentDetails());
        transaction.setNotes(dto.getNotes());
        transaction.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);
        return transaction;
    }
}
