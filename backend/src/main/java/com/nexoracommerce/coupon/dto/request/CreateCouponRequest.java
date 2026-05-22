package com.nexoracommerce.coupon.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateCouponRequest(
    @NotBlank(message = "Coupon code cannot be empty")
    @Size(min = 1, max = 50, message = "Coupon code must be between 1 and 50 characters")
    String code,

    @NotNull(message = "Discount percent is required")
    @Min(value = 1, message = "Discount percent must be between 1 and 100")
    @Max(value = 100, message = "Discount percent must be between 1 and 100")
    Integer discountPercent,

    Boolean active,

    Long minimumOrderAmount,  // VND

    LocalDateTime expiryDate  // null = no expiry
) {
    public CreateCouponRequest {
        if (active == null) {
            active = true;
        }
        if (minimumOrderAmount == null) {
            minimumOrderAmount = 0L;
        }
    }
}
