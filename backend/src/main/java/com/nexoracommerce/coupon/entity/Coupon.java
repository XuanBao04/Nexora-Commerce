package com.nexoracommerce.coupon.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "code")
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_active_expiry", columnList = "active, expiry_date")
})
public class Coupon {
    @Id
    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code must not exceed 50 characters")
    @Column(name = "code", length = 50)
    private String code;

    @NotNull(message = "Discount percentage is required")
    @Min(value = 1, message = "Discount must be at least 1%")
    @Max(value = 100, message = "Discount must not exceed 100%")
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @NotNull(message = "Active flag is required")
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @NotNull(message = "Minimum order amount is required")
    @Min(value = 0, message = "Minimum order amount must be non-negative")
    @Column(name = "minimum_order_amount", nullable = false)
    @Builder.Default
    private Long minimumOrderAmount = 0L;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}
