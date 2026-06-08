package com.nexoracommerce.order.repository;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface OrderStatusHistoryRepository extends BaseRepository<OrderStatusHistory, Long> {

    /**
     * Find all status history records for an order
     */
    List<OrderStatusHistory> findByOrder_Id(String orderId);

    /**
     * Find all status history records for an order, ordered by creation date (newest first)
     */
    List<OrderStatusHistory> findByOrder_IdOrderByCreatedAtDesc(String orderId);

    /**
     * Find the latest status change for an order
     */
    @Query("""
        SELECT osh 
        FROM OrderStatusHistory osh 
        WHERE osh.order.id = :orderId 
        ORDER BY osh.createdAt DESC 
        LIMIT 1
    """)
    Optional<OrderStatusHistory> findLatestStatusChangeByOrder(@Param("orderId") String orderId);

    /**
     * Find when order reached a specific status
     */
    @Query("""
        SELECT osh 
        FROM OrderStatusHistory osh 
        WHERE osh.order.id = :orderId 
          AND osh.status = :status 
        ORDER BY osh.createdAt ASC 
        LIMIT 1
    """)
    Optional<OrderStatusHistory> findFirstStatusChange(@Param("orderId") String orderId,
                                                       @Param("status") OrderStatus status);

    /**
     * Find all status changes in a date range
     */
    List<OrderStatusHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find all status changes by a specific user/system
     */
    List<OrderStatusHistory> findByChangedBy(String changedBy);

    /**
     * Find all transitions to a specific status
     */
    List<OrderStatusHistory> findByStatus(OrderStatus status);

    /**
     * Count how many times an order changed status
     */
    long countByOrder_Id(String orderId);

    /**
     * Check if order has reached a specific status
     */
    boolean existsByOrder_IdAndStatus(String orderId, OrderStatus status);

    /**
     * Find all orders that reached a specific status by a certain date
     */
    @Query("""
        SELECT osh 
        FROM OrderStatusHistory osh 
        WHERE osh.status = :status 
          AND osh.createdAt <= :date 
        ORDER BY osh.createdAt DESC
    """)
    List<OrderStatusHistory> findStatusReachedByDate(@Param("status") OrderStatus status,
                                                     @Param("date") LocalDateTime date);
}
