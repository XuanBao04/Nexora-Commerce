package com.nexoracommerce.order.dto.response;

/**
 * DTO for order item response
 * Contains snapshot of product/variant at checkout time
 */
public record OrderItemResponse(
    Long id,
    String variantSku,                 // Product variant SKU
    String productName,                // Snapshot of product name at checkout
    String variantName,                // Snapshot of variant name (color, size, etc.) at checkout
    Integer quantity,                   // Quantity ordered
    Long price                          // Price snapshot at checkout
) {}
