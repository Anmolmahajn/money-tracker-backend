package com.moneytracker.repository;

import com.moneytracker.model.PaymentMethod;
import com.moneytracker.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);
    
    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);
    
    List<Transaction> findByUserIdAndPaymentMethod(Long userId, PaymentMethod paymentMethod);
    
    List<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Transaction> findByCategoryId(Long categoryId);
    
    List<Transaction> findByPaymentMethod(PaymentMethod paymentMethod);
    
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findUserTransactionsInDateRange(@Param("userId") Long userId, 
                                                      @Param("startDate") LocalDate startDate, 
                                                      @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getUserTotalSpendingInDateRange(@Param("userId") Long userId,
                                               @Param("startDate") LocalDate startDate, 
                                               @Param("endDate") LocalDate endDate);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSpendingInDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.category.name")
    List<Object[]> getUserSpendingByCategory(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate, 
                                            @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.category.name")
    List<Object[]> getSpendingByCategory(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t.paymentMethod, SUM(t.amount) FROM Transaction t WHERE t.user.id = :userId AND t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.paymentMethod")
    List<Object[]> getUserSpendingByPaymentMethod(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
    
    @Query("SELECT t.paymentMethod, SUM(t.amount) FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate GROUP BY t.paymentMethod")
    List<Object[]> getSpendingByPaymentMethod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
