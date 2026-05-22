package com.nexoracommerce.order.dto.response;

import java.util.List;

/**
 * DTO trả về khi preview đơn hàng trước khi checkout.
 * Cho user xem tổng tiền, phí ship, discount trước khi xác nhận đặt hàng.
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
