package com.nexoracommerce.order.dto.response;

public record OrderItemResponse(
    Long id,
    String productId,
    Integer quantity,
    Long price
) {}
