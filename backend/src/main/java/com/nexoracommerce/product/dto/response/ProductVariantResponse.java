package com.nexoracommerce.product.dto.response;

import java.util.List;

public record ProductVariantResponse(
    String sku,
    Long price,
    Integer quantity,
    Integer reservedQuantity,
    Integer soldQuantity,
    List<ProductVariantAttributeResponse> attributes
) {}
