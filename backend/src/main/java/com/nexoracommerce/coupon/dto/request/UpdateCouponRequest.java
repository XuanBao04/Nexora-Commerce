package com.nexoracommerce.coupon.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCouponRequest {

    @Min(value = 1, message = "Discount percent must be >= 1")
    @Max(value = 100, message = "Discount percent must be <= 100")
    private Integer discountPercent;

    private Boolean active;

    @Min(value = 0, message = "Minimum order amount must be >= 0")
    private Long minimumOrderAmount;

    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;
}
