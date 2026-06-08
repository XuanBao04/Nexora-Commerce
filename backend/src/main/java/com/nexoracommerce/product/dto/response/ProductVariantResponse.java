package com.nexoracommerce.product.dto.response;

import java.util.List;

public record ProductVariantResponse(
    String sku,
    String productName,
    String imageUrl,
    Long price,
    Integer quantity,
    Integer reservedQuantity,
    Integer soldQuantity,
    List<ProductVariantAttributeResponse> attributes
) {}
