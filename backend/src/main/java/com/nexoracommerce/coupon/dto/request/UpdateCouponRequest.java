package com.nexoracommerce.coupon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public record UpdateCouponRequest(
    @Min(value = 1, message = "Discount percent must be >= 1")
    @Max(value = 100, message = "Discount percent must be <= 100")
    Integer discountPercent,

    Boolean active,

    @Min(value = 0, message = "Minimum order amount must be >= 0")
    Long minimumOrderAmount,

    @Future(message = "Expiry date must be in the future")
    LocalDateTime expiryDate
) {}
