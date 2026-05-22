package com.nexoracommerce.coupon.dto.response;

import java.time.LocalDateTime;

public record CouponResponse(
    String code,
    Integer discountPercent,
    Boolean active,
    Long minimumOrderAmount,
    LocalDateTime expiryDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
