package com.nexoracommerce.inventory.dto.response;

import lombok.Builder;

@Builder
public record InventoryResponse(
    Long id,
    String productId,
    Integer quantity,
    Integer reservedQuantity,
    Integer soldQuantity,
    Integer availableQuantity
) {}
