package com.nexoracommerce.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_transactions_id_seq")
    @SequenceGenerator(name = "payment_transactions_id_seq", sequenceName = "payment_transactions_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // 'COD', 'VNPAY', 'MOMO'

    @Column(nullable = false)
    private Long amount;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId; // Reference ID from VNPAY/Momo

    @Column(nullable = false, length = 50)
    private String status; // 'PENDING', 'SUCCESS', 'FAILED'

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
