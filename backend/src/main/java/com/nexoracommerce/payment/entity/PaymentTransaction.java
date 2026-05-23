package com.nexoracommerce.payment.entity;

import com.nexoracommerce.order.entity.Order;
import jakarta.persistence.*;
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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "order")
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_trans_order", columnList = "order_id")
})
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank(message = "Payment method is required")
    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // 'COD', 'VNPAY', 'MOMO'

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be non-negative")
    @Column(nullable = false)
    private Long amount;

    @Size(max = 255, message = "Provider transaction ID must not exceed 255 characters")
    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId; // Reference ID from VNPAY/Momo

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    @Column(nullable = false, length = 50)
    private String status; // 'PENDING', 'SUCCESS', 'FAILED'

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

