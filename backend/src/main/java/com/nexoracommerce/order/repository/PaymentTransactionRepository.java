package com.nexoracommerce.order.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.payment.enums.PaymentMethod;
import com.nexoracommerce.payment.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction, Long> {

    /**
     * Find all payment transactions for an order
     */
    List<PaymentTransaction> findByOrder_Id(String orderId);

    /**
     * Find all payment transactions for an order, ordered by creation date (newest first)
     */
    List<PaymentTransaction> findByOrder_IdOrderByCreatedAtDesc(String orderId);

    /**
     * Find the latest payment transaction for an order
     */
    @Query("""
        SELECT pt 
        FROM PaymentTransaction pt 
        WHERE pt.order.id = :orderId 
        ORDER BY pt.createdAt DESC 
        LIMIT 1
    """)
    Optional<PaymentTransaction> findLatestPaymentTransaction(@Param("orderId") String orderId);

    /**
     * Find successful payment transaction for an order
     */
    Optional<PaymentTransaction> findByOrder_IdAndStatus(String orderId, TransactionStatus status);

    /**
     * Find all payment transactions with a specific status
     */
    List<PaymentTransaction> findByStatus(TransactionStatus status);

    /**
     * Find all payment transactions by payment method
     */
    List<PaymentTransaction> findByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Find payment transactions by payment method with pagination
     */
    Page<PaymentTransaction> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Find all payment transactions by provider transaction ID
     */
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    /**
     * Count successful transactions
     */
    long countByStatus(TransactionStatus status);

    /**
     * Count transactions by payment method
     */
    long countByPaymentMethod(PaymentMethod paymentMethod);

    /**
     * Sum of successful payment amounts
     */
    @Query("""
        SELECT COALESCE(SUM(pt.amount), 0) 
        FROM PaymentTransaction pt 
        WHERE pt.status = :status
    """)
    long getTotalAmountByStatus(@Param("status") TransactionStatus status);

    /**
     * Sum of payment amounts by payment method
     */
    @Query("""
        SELECT COALESCE(SUM(pt.amount), 0) 
        FROM PaymentTransaction pt 
        WHERE pt.paymentMethod = :paymentMethod
    """)
    long getTotalAmountByPaymentMethod(@Param("paymentMethod") PaymentMethod paymentMethod);

    /**
     * Find payment transactions in a date range
     */
    Page<PaymentTransaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find successful transactions in a date range
     */
    @Query("""
        SELECT pt 
        FROM PaymentTransaction pt 
        WHERE pt.status = :status 
          AND pt.createdAt BETWEEN :startDate AND :endDate 
        ORDER BY pt.createdAt DESC
    """)
    Page<PaymentTransaction> findSuccessfulTransactionsByDateRange(@Param("status") TransactionStatus status,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate,
                                                                    Pageable pageable);

    /**
     * Find failed transactions
     */
    @Query("""
        SELECT pt 
        FROM PaymentTransaction pt 
        WHERE pt.status = :status 
        ORDER BY pt.createdAt DESC
    """)
    Page<PaymentTransaction> findFailedTransactions(@Param("status") TransactionStatus status, Pageable pageable);

    /**
     * Check if order has a successful payment
     */
    boolean existsByOrder_IdAndStatus(String orderId, TransactionStatus status);

    /**
     * Count payment attempts for an order
     */
    long countByOrder_Id(String orderId);

    /**
     * Get average transaction amount
     */
    @Query("""
        SELECT COALESCE(AVG(pt.amount), 0) 
        FROM PaymentTransaction pt
    """)
    double getAverageTransactionAmount();

    /**
     * Get average transaction amount by payment method
     */
    @Query("""
        SELECT COALESCE(AVG(pt.amount), 0) 
        FROM PaymentTransaction pt 
        WHERE pt.paymentMethod = :paymentMethod
    """)
    double getAverageTransactionAmountByPaymentMethod(@Param("paymentMethod") PaymentMethod paymentMethod);
}
