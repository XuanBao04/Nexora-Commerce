package com.nexoracommerce.statistic.dto.response;

public record BestSellerItem(
    String productName,
    String sku,
    Integer soldQuantity,
    Long revenue
) {}
