package com.nexoracommerce.order.dto.response;

import com.nexoracommerce.common.enums.OrderStatus;
import java.time.LocalDateTime;

/**
 * DTO for order status history response
 * Tracks status changes and audit trail
 */
public record OrderStatusHistoryResponse(
    Long id,
    String orderId,
    OrderStatus status,                 // Order status: PENDING, PROCESSING, SHIPPING, DELIVERED, CANCELLED
    String changedBy,                   // Username, ADMIN, or SYSTEM
    String reason,                      // Reason for status change
    LocalDateTime createdAt
) {}
