package com.nexoracommerce.cart.entity;

import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@ToString(exclude = {"user", "variant"})
@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "variant_sku"})
}, indexes = {
    @Index(name = "idx_cart_items_user", columnList = "user_id"),
    @Index(name = "idx_cart_items_variant", columnList = "variant_sku")
})
@EntityListeners(AuditingEntityListener.class)
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_items_id_seq")
    @SequenceGenerator(name = "cart_items_id_seq", sequenceName = "cart_items_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Product variant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_sku", nullable = false)
    private ProductVariant variant;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Transient
    private String productId;

    @Transient
    public String getProductId() {
        return variant != null ? variant.getSku() : null;
    }
}
