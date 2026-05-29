package com.nexoracommerce.order.dto.response;

import java.util.List;

/**
 * DTO for order preview response
 * Displayed before checkout - shows total price, shipping fee, discount
 */
public record OrderPreviewResponse(
    String userId,
    List<OrderItemResponse> items,
    Long subtotal,
    Long discountAmount,
    Long shippingFee,
    Long totalPrice,
    String couponCode
) {}
