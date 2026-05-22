package com.nexoracommerce.product.dto.response;

public record ProductResponse(
    String id,
    String name,
    String description,
    Long price,
    String status,
    String imageUrl,
    String imagePublicId
) {}
