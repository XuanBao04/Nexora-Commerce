package com.nexoracommerce.order.dto.request;

import com.nexoracommerce.common.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for order status change request
 * Used by admin to update order status with reason
 */
public record OrderStatusChangeRequest(
    @NotBlank(message = "Order ID is required")
    String orderId,
    
    @NotNull(message = "New status is required")
    OrderStatus newStatus,              // Target status
    
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    String reason                       // Optional: Reason for status change
) {}
