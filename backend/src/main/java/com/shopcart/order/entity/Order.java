package com.shopcart.order.entity;

import com.shopcart.common.enums.OrderStatus;
import com.shopcart.coupon.entity.Coupon;
import com.shopcart.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @Column(name = "id", length = 50)
    private String id; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice; 

    @Column(name = "shipping_fee", nullable = false)
    @Builder.Default
    private Long shippingFee = 0L;

    @Column(name = "discount_amount", nullable = false)
    @Builder.Default
    private Long discountAmount = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_code")
    private Coupon coupon;

    @Column(name = "shipping_address", nullable = false, length = 255)
    private String shippingAddress;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status; 

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "last_modified_date", nullable = false)
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();
}
