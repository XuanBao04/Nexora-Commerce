package com.nexoracommerce.redis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockQuantityRequest(
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
    Long quantity
) {}
