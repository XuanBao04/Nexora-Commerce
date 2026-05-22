package com.nexoracommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
    @NotBlank(message = "Product ID is required")
    String productId,

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    Long price
) {}
