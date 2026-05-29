package com.nexoracommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO for order item request
 * Contains variant details and quantity
 */
public record OrderItemRequest(
    @NotBlank(message = "Variant SKU is required")
    String variantSku,
    
    @NotBlank(message = "Product name snapshot is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    String productName,              // Snapshot of product name at checkout
    
    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    String variantName,              // Optional: Snapshot of variant info at checkout

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    Integer quantity,

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    Long price                       // Price snapshot at checkout
) {}
