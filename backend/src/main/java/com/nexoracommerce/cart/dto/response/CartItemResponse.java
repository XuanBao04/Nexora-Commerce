package com.nexoracommerce.cart.dto.response;

import java.time.LocalDateTime;

public record CartItemResponse(
    Long id,
    String productId,
    Integer quantity,
    Long price,
    Long totalPrice,
    LocalDateTime createdAt
) {}
