package com.nexoracommerce.order.entity;

import com.nexoracommerce.common.enums.OrderStatus;
import jakarta.persistence.*;
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
@Table(name = "order_status_history", indexes = {
    @Index(name = "idx_order_history_order", columnList = "order_id")
})
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_status_history_id_seq")
    @SequenceGenerator(name = "order_status_history_id_seq", sequenceName = "order_status_history_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @NotBlank(message = "Changed by user/system is required")
    @Size(max = 100, message = "Changed by username must not exceed 100 characters")
    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy; // 'SYSTEM', 'ADMIN', or username

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Column(length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
