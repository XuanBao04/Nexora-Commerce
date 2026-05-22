package com.nexoracommerce.coupon.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "coupons")
public class Coupon {
    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "minimum_order_amount", nullable = false)
    @Builder.Default
    private Long minimumOrderAmount = 0L;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}
