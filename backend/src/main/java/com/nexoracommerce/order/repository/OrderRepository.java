package com.nexoracommerce.order.repository;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface OrderRepository extends BaseRepository<Order, String> {

    // ========== Basic Queries ==========

    List<Order> findByUser_Id(UUID userId);

    List<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    // ========== Fetch Queries (with relationships) ==========

    @Query("""
        SELECT DISTINCT o 
        FROM Order o 
        LEFT JOIN FETCH o.orderItems
    """)
    List<Order> findAllWithItems();

    @Query("""
        SELECT DISTINCT o 
        FROM Order o 
        LEFT JOIN FETCH o.orderItems 
        WHERE o.id = :orderId
    """)
    Optional<Order> findByIdWithItems(@Param("orderId") String orderId);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.orderItems
        JOIN FETCH o.user
        WHERE o.id = :orderId
    """)
    Optional<Order> findByIdWithItemsAndUser(@Param("orderId") String orderId);


    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.orderItems
        WHERE o.user.id = :userId
        ORDER BY o.createdAt DESC
    """)
    List<Order> findByUserIdOrderByCreatedAtDescWithItems(@Param("userId") UUID userId);

    // ========== Pagination Queries ==========

    /**
     * Tìm kiếm và lọc đơn hàng linh hoạt
     */
    @Query(
        value = """
            SELECT o 
            FROM Order o 
            WHERE (CAST(:userId AS uuid) IS NULL OR o.user.id = :userId)
              AND (:status IS NULL OR o.status = :status)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (:couponCode IS NULL OR o.coupon.code = :couponCode)
              AND (CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)
              AND (:minPrice IS NULL OR o.totalPrice >= :minPrice)
        """,
        countQuery = """
            SELECT COUNT(o) 
            FROM Order o 
            WHERE (CAST(:userId AS uuid) IS NULL OR o.user.id = :userId)
              AND (:status IS NULL OR o.status = :status)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (:couponCode IS NULL OR o.coupon.code = :couponCode)
              AND (CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate)
              AND (CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)
              AND (:minPrice IS NULL OR o.totalPrice >= :minPrice)
        """
    )
    Page<Order> findOrdersWithFilters(
            @Param("userId") UUID userId,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("couponCode") String couponCode,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minPrice") Long minPrice,
            Pageable pageable);

    // ========== Count Queries ==========

    /**
     * Count orders by status
     */
    long countByStatus(OrderStatus status);

    /**
     * Count orders by payment status
     */
    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * Count orders by user
     */
    long countByUser_Id(UUID userId);

    /**
     * Count unpaid orders
     */
    @Query("""
        SELECT COUNT(o) 
        FROM Order o 
        WHERE o.paymentStatus = :paymentStatus
    """)
    long countUnpaidOrders(@Param("paymentStatus") PaymentStatus paymentStatus);

    /**
     * Count orders by status for a user
     */
    long countByUser_IdAndStatus(UUID userId, OrderStatus status);

    // ========== Aggregation Queries ==========

    /**
     * Total revenue (sum of all order totals)
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0) 
        FROM Order o
    """)
    long getTotalRevenue();

    /**
     * Average order value
     */
    @Query("""
        SELECT COALESCE(AVG(o.totalPrice), 0) 
        FROM Order o
    """)
    double getAverageOrderValue();

    /**
     * Total revenue by status
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0) 
        FROM Order o 
        WHERE o.status = :status
    """)
    long getTotalRevenueByStatus(@Param("status") OrderStatus status);

    /**
     * Total net revenue (sum of all paid order totals)
     */
    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0) 
        FROM Order o 
        WHERE o.paymentStatus = 'PAID'
    """)
    long getTotalRevenueByPaymentStatusPaid();

    /**
     * Daily revenue aggregation for paid orders within a start date
     */
    @Query("""
        SELECT CAST(o.createdAt AS LocalDate), COALESCE(SUM(o.totalPrice), 0) 
        FROM Order o 
        WHERE o.paymentStatus = 'PAID' 
          AND o.createdAt >= :startDate 
        GROUP BY CAST(o.createdAt AS LocalDate) 
        ORDER BY CAST(o.createdAt AS LocalDate) ASC
    """)
    List<Object[]> findRevenueByDateRange(@Param("startDate") LocalDateTime startDate);

    /**
     * Count of orders grouped by status
     */
    @Query("""
        SELECT o.status, COUNT(o) 
        FROM Order o 
        GROUP BY o.status
    """)
    List<Object[]> getOrderStatusStats();

    @Query("""
        SELECT o
        FROM Order o
        WHERE o.user.id = :userId
        ORDER BY o.createdAt DESC
    """)
    List<Order> getRecentOrdersByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        LEFT JOIN FETCH o.orderItems
        JOIN FETCH o.paymentTransactions pt
        WHERE o.status = com.nexoracommerce.common.enums.OrderStatus.PENDING
          AND o.paymentStatus = com.nexoracommerce.payment.enums.PaymentStatus.UNPAID
          AND pt.paymentMethod = com.nexoracommerce.payment.enums.PaymentMethod.VNPAY
          AND o.createdAt <= :thresholdTime
    """)
    List<Order> findExpiredUnpaidVnPayOrders(@Param("thresholdTime") LocalDateTime thresholdTime);
}
