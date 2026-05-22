package com.nexoracommerce.cart.dto.response;

import java.util.List;

public record CartResponse(
    String userId,
    List<CartItemResponse> items,
    Integer totalItems,
    Long totalPrice
) {}