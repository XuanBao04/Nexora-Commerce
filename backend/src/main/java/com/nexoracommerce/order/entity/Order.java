package com.nexoracommerce.order.entity;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.coupon.entity.Coupon;
import com.nexoracommerce.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
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
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "coupon", "orderItems", "statusHistory"})
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_user", columnList = "user_id"),
    @Index(name = "idx_orders_coupon", columnList = "coupon_code")
})
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @NotBlank(message = "Order ID is required")
    @Size(max = 50, message = "Order ID must not exceed 50 characters")
    @Column(name = "id", length = 50)
    private String id; 

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Total price is required")
    @Min(value = 0, message = "Total price must be non-negative")
    @Column(name = "total_price", nullable = false)
    private Long totalPrice; 

    @NotNull(message = "Shipping fee is required")
    @Min(value = 0, message = "Shipping fee must be non-negative")
    @Column(name = "shipping_fee", nullable = false)
    @Builder.Default
    private Long shippingFee = 0L;

    @NotNull(message = "Discount amount is required")
    @Min(value = 0, message = "Discount amount must be non-negative")
    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_code")
    private Coupon coupon;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    @Column(name = "shipping_address", nullable = false, length = 255)
    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotNull(message = "Order status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status; 

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "last_modified_date", nullable = false)
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();
}
