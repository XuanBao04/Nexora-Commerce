package com.nexoracommerce.product.dto.request;

import java.util.List;

public record ProductVariantRequest(
    String sku,
    Long price,
    Integer quantity,
    List<ProductVariantAttributeRequest> attributes
) {}
