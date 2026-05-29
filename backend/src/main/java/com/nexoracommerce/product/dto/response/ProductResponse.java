package com.nexoracommerce.product.dto.response;

public record ProductResponse(
    String id,
    String name,
    String description,
    Long price,
    Integer quantity,
    String status,
    String imageUrl,
    String imagePublicId,
    java.util.List<ProductVariantResponse> variants
) {}
