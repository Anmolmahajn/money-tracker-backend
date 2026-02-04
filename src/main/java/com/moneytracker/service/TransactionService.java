package com.moneytracker.service;

import com.moneytracker.dto.SpendingSummaryDTO;
import com.moneytracker.dto.TransactionDTO;
import com.moneytracker.exception.ResourceNotFoundException;
import com.moneytracker.model.Category;
import com.moneytracker.model.PaymentMethod;
import com.moneytracker.model.Transaction;
import com.moneytracker.repository.CategoryRepository;
import com.moneytracker.repository.TransactionRepository;
import com.moneytracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.moneytracker.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;



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


    @Transactional(readOnly = true)
    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TransactionDTO getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return convertToDTO(transaction);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findTransactionsInDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByCategory(Long categoryId) {
        return transactionRepository.findByCategoryId(categoryId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDTO> getTransactionsByPaymentMethod(PaymentMethod paymentMethod) {
        return transactionRepository.findByPaymentMethod(paymentMethod).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = convertToEntity(transactionDTO);
        Transaction savedTransaction = transactionRepository.save(transaction);
        return convertToDTO(savedTransaction);
    }
    
    @Transactional
    public TransactionDTO updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        
        Category category = categoryRepository.findById(transactionDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + transactionDTO.getCategoryId()));
        
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
    
    @Transactional
    public void deleteTransaction(Long id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found with id: " + id);
        }
        transactionRepository.deleteById(id);
    }
    
    @Transactional(readOnly = true)
    public SpendingSummaryDTO getSpendingSummary(LocalDate startDate, LocalDate endDate) {
        SpendingSummaryDTO summary = new SpendingSummaryDTO();
        
        // Total spending
        BigDecimal totalSpending = transactionRepository.getTotalSpendingInDateRange(startDate, endDate);
        summary.setTotalSpending(totalSpending != null ? totalSpending : BigDecimal.ZERO);
        
        // Category breakdown
        List<Object[]> categoryData = transactionRepository.getSpendingByCategory(startDate, endDate);
        Map<String, BigDecimal> categoryBreakdown = new HashMap<>();
        for (Object[] row : categoryData) {
            categoryBreakdown.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.setCategoryBreakdown(categoryBreakdown);
        
        // Payment method breakdown
        List<Object[]> paymentData = transactionRepository.getSpendingByPaymentMethod(startDate, endDate);
        Map<String, BigDecimal> paymentBreakdown = new HashMap<>();
        for (Object[] row : paymentData) {
            PaymentMethod method = (PaymentMethod) row[0];
            paymentBreakdown.put(method.getDisplayName(), (BigDecimal) row[1]);
        }
        summary.setPaymentMethodBreakdown(paymentBreakdown);
        
        // Transaction count
        List<Transaction> transactions = transactionRepository.findTransactionsInDateRange(startDate, endDate);
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Transaction transaction = new Transaction();

        transaction.setUser(user);

        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setPaymentMethod(dto.getPaymentMethod());
        transaction.setPaymentDetails(dto.getPaymentDetails());
        transaction.setNotes(dto.getNotes());
        transaction.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);

        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        transaction.setCategory(category);

        return transaction;
    }

}
