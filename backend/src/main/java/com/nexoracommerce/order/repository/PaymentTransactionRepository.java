package com.nexoracommerce.order.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.order.enums.PaymentMethod;
import com.nexoracommerce.order.enums.TransactionStatus;
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
            select pt
            from PaymentTransaction pt
            where pt.order.id = :orderId
            order by pt.createdAt desc
            limit 1
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
            select coalesce(sum(pt.amount), 0)
            from PaymentTransaction pt
            where pt.status = :status
            """)
    long getTotalAmountByStatus(@Param("status") TransactionStatus status);

    /**
     * Sum of payment amounts by payment method
     */
    @Query("""
            select coalesce(sum(pt.amount), 0)
            from PaymentTransaction pt
            where pt.paymentMethod = :paymentMethod
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
            select pt
            from PaymentTransaction pt
            where pt.status = :status and pt.createdAt between :startDate and :endDate
            order by pt.createdAt desc
            """)
    Page<PaymentTransaction> findSuccessfulTransactionsByDateRange(@Param("status") TransactionStatus status,
                                                                    @Param("startDate") LocalDateTime startDate,
                                                                    @Param("endDate") LocalDateTime endDate,
                                                                    Pageable pageable);

    /**
     * Find failed transactions
     */
    @Query("""
            select pt
            from PaymentTransaction pt
            where pt.status = :status
            order by pt.createdAt desc
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
    @Query("select coalesce(avg(pt.amount), 0) from PaymentTransaction pt")
    double getAverageTransactionAmount();

    /**
     * Get average transaction amount by payment method
     */
    @Query("""
            select coalesce(avg(pt.amount), 0)
            from PaymentTransaction pt
            where pt.paymentMethod = :paymentMethod
            """)
    double getAverageTransactionAmountByPaymentMethod(@Param("paymentMethod") PaymentMethod paymentMethod);
}
