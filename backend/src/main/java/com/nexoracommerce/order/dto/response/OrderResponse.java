package com.nexoracommerce.order.dto.response;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.order.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for order response
 * Contains complete order details with items, pricing, status, and payment info
 */
public record OrderResponse(
    String id,
    String userId,
    List<OrderItemResponse> items,
    Long subtotal,
    Long discountAmount,
    Long shippingFee,
    Long totalPrice,
    String couponCode,
    OrderStatus status,              // Order status: PENDING, PROCESSING, SHIPPING, DELIVERED, CANCELLED
    PaymentStatus paymentStatus,    // Payment status: UNPAID, PAID, REFUNDED
    String customerNote,             // Customer notes at checkout
    LocalDateTime createdAt,
    LocalDateTime lastModifiedDate,
    
    // Shipping address fields
    String shippingAddress,
    String phoneNumber
) {}
