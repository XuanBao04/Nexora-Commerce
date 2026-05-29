package com.nexoracommerce.order.entity;

import com.nexoracommerce.order.enums.PaymentMethod;
import com.nexoracommerce.order.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
    @Index(name = "idx_payment_trans_order", columnList = "order_id"),
    @Index(name = "idx_payment_trans_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod; // 'COD', 'VNPAY', 'MOMO'

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be non-negative")
    @Column(nullable = false)
    private Long amount; // Amount in VND

    @Size(max = 255, message = "Provider transaction ID must not exceed 255 characters")
    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId; // Reference ID from payment provider

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionStatus status; // 'PENDING', 'SUCCESS', 'FAILED'

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
}
