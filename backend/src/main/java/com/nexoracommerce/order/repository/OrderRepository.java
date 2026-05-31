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

    @Query("select distinct o from Order o left join fetch o.orderItems")
    List<Order> findAllWithItems();

    @Query("select distinct o from Order o left join fetch o.orderItems where o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") String orderId);

    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems
            join fetch o.user
            where o.id = :orderId
            """)
    Optional<Order> findByIdWithItemsAndUser(@Param("orderId") String orderId);


    @Query("""
            select distinct o
            from Order o
            left join fetch o.orderItems
            where o.user.id = :userId
            order by o.createdAt desc
            """)
    List<Order> findByUserIdOrderByCreatedAtDescWithItems(@Param("userId") UUID userId);

    // ========== Pagination Queries ==========

    /**
     * Find all orders with pagination (for admin)
     */
    @Query(value = "select o from Order o order by o.createdAt desc",
           countQuery = "select count(o) from Order o")
    Page<Order> findAllWithItemsPageable(Pageable pageable);

    /**
     * Find orders by user ID with pagination
     */
    @Query(value = """
            select o
            from Order o
            where o.user.id = :userId
            order by o.createdAt desc
            """,
           countQuery = "select count(o) from Order o where o.user.id = :userId")
    Page<Order> findByUserIdWithItemsPageable(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find orders by status with pagination
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders by user ID and status with pagination
     */
    Page<Order> findByUser_IdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    /**
     * Find orders by payment status with pagination
     */
    Page<Order> findByPaymentStatus(PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Find orders by user ID and payment status
     */
    Page<Order> findByUser_IdAndPaymentStatus(UUID userId, PaymentStatus paymentStatus, Pageable pageable);

    /**
     * Find unpaid orders for a user
     */
    @Query("""
            select o
            from Order o
            where o.user.id = :userId and o.paymentStatus = :paymentStatus
            order by o.createdAt desc
            """)
    Page<Order> findUnpaidOrdersByUser(@Param("userId") UUID userId, 
                                       @Param("paymentStatus") PaymentStatus paymentStatus,
                                       Pageable pageable);

    /**
     * Find orders by date range
     */
    Page<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find orders by user and date range
     */
    Page<Order> findByUser_IdAndCreatedAtBetween(UUID userId, LocalDateTime startDate, 
                                                 LocalDateTime endDate, Pageable pageable);

    /**
     * Find orders with total price greater than threshold
     */
    Page<Order> findByTotalPriceGreaterThan(Long minPrice, Pageable pageable);

    /**
     * Find orders by coupon code
     */
    @Query("""
            select o
            from Order o
            where o.coupon.code = :couponCode
            order by o.createdAt desc
            """)
    Page<Order> findByCouponCode(@Param("couponCode") String couponCode, Pageable pageable);

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
    @Query("select count(o) from Order o where o.paymentStatus = :paymentStatus")
    long countUnpaidOrders(@Param("paymentStatus") PaymentStatus paymentStatus);

    /**
     * Count orders by status for a user
     */
    long countByUser_IdAndStatus(UUID userId, OrderStatus status);

    // ========== Aggregation Queries ==========

    /**
     * Total revenue (sum of all order totals)
     */
    @Query("select coalesce(sum(o.totalPrice), 0) from Order o")
    long getTotalRevenue();

    /**
     * Average order value
     */
    @Query("select coalesce(avg(o.totalPrice), 0) from Order o")
    double getAverageOrderValue();

    /**
     * Total revenue by status
     */
    @Query("select coalesce(sum(o.totalPrice), 0) from Order o where o.status = :status")
    long getTotalRevenueByStatus(@Param("status") OrderStatus status);

    /**
     * Total net revenue (sum of all paid order totals)
     */
    @Query("select coalesce(sum(o.totalPrice), 0) from Order o where o.paymentStatus = 'PAID'")
    long getTotalRevenueByPaymentStatusPaid();

    /**
     * Daily revenue aggregation for paid orders within a start date
     */
    @Query("select cast(o.createdAt as LocalDate), coalesce(sum(o.totalPrice), 0) " +
           "from Order o " +
           "where o.paymentStatus = 'PAID' and o.createdAt >= :startDate " +
           "group by cast(o.createdAt as LocalDate) " +
           "order by cast(o.createdAt as LocalDate) asc")
    List<Object[]> findRevenueByDateRange(@Param("startDate") LocalDateTime startDate);

    /**
     * Count of orders grouped by status
     */
    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> getOrderStatusStats();

    /**
     * Get recent orders for user (last N orders)
     */
    @Query("""
            select o
            from Order o
            where o.user.id = :userId
            order by o.createdAt desc
            """)
    List<Order> getRecentOrdersByUser(@Param("userId") UUID userId, Pageable pageable);

}
