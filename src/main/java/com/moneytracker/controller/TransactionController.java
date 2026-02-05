package com.moneytracker.controller;

import com.moneytracker.dto.SpendingSummaryDTO;
import com.moneytracker.dto.TransactionDTO;
import com.moneytracker.model.PaymentMethod;
import com.moneytracker.service.TransactionService;
import com.moneytracker.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    /**
     * ✅ SECURE - Get only current user's transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<TransactionDTO> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * ✅ SECURE - Get transaction only if belongs to current user
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        TransactionDTO transaction = transactionService.getTransactionById(id, userId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * ✅ SECURE - Get user's transactions by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<TransactionDTO> transactions = transactionService.getUserTransactionsByDateRange(
                userId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    /**
     * ✅ SECURE - Get user's transactions by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<TransactionDTO> transactions = transactionService.getUserTransactionsByCategory(
                userId, categoryId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * ✅ SECURE - Get user's transactions by payment method
     */
    @GetMapping("/payment-method/{paymentMethod}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByPaymentMethod(
            @PathVariable PaymentMethod paymentMethod,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        List<TransactionDTO> transactions = transactionService.getUserTransactionsByPaymentMethod(
                userId, paymentMethod);
        return ResponseEntity.ok(transactions);
    }

    /**
     * ✅ SECURE - Create transaction for current user
     */
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @Valid @RequestBody TransactionDTO transactionDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        TransactionDTO createdTransaction = transactionService.createTransaction(transactionDTO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }

    /**
     * ✅ SECURE - Update only if transaction belongs to current user
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDTO transactionDTO,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        TransactionDTO updatedTransaction = transactionService.updateTransaction(id, transactionDTO, userId);
        return ResponseEntity.ok(updatedTransaction);
    }

    /**
     * ✅ SECURE - Delete only if transaction belongs to current user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ SECURE - Get spending summary for current user
     */
    @GetMapping("/summary")
    public ResponseEntity<SpendingSummaryDTO> getSpendingSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        SpendingSummaryDTO summary = transactionService.getUserSpendingSummary(userId, startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
