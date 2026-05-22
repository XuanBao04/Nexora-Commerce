package com.nexoracommerce.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    String id,
    String userId,
    List<OrderItemResponse> items,
    Long subtotal,         
    Long discountAmount,  
    Long shippingFee,
    Long totalPrice,       
    String couponCode,     
    String status,
    LocalDateTime createdAt,
    LocalDateTime lastModifiedDate,

    // Shipping address fields
    String shippingAddress,
    String city,
    String district,
    String ward,
    String postalCode,
    String phoneNumber
) {}
