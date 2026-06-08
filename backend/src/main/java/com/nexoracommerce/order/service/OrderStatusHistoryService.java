package com.nexoracommerce.order.service;

import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Order Status History tracking
 */
public interface OrderStatusHistoryService {

    /**
     * Record a status change for an order
     * @param orderId the order ID
     * @param newStatus the new status
     * @param changedBy username or SYSTEM
     * @param reason optional reason for change
     * @return created OrderStatusHistoryResponse
     */
    OrderStatusHistoryResponse recordStatusChange(String orderId, String newStatus, String changedBy, String reason);

    /**
     * Get all status changes for an order
     * @param orderId the order ID
     * @return list of OrderStatusHistoryResponse ordered by date (newest first)
     */
    List<OrderStatusHistoryResponse> getOrderStatusHistory(String orderId);

    /**
     * Get the latest status change for an order
     * @param orderId the order ID
     * @return OrderStatusHistoryResponse or null if not found
     */
    OrderStatusHistoryResponse getLatestStatusChange(String orderId);

    /**
     * Get status history with pagination
     * @param orderId the order ID
     * @param pageable pagination parameters
     * @return Page of OrderStatusHistoryResponse
     */
    Page<OrderStatusHistoryResponse> getOrderStatusHistoryPageable(String orderId, Pageable pageable);

    /**
     * Check if order has reached a specific status
     * @param orderId the order ID
     * @param status the status to check
     * @return true if order has reached this status, false otherwise
     */
    boolean hasReachedStatus(String orderId, String status);

    /**
     * Get number of status changes for an order
     * @param orderId the order ID
     * @return count of status changes
     */
    long getStatusChangeCount(String orderId);
}
