package com.nexoracommerce.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateQuantityRequest(
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity
) {}
