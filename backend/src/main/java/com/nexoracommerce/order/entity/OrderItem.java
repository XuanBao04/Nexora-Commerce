package com.nexoracommerce.order.entity;

import com.nexoracommerce.product.entity.ProductVariant;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"order", "variant"})
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_items_order", columnList = "order_id"),
    @Index(name = "idx_order_items_variant", columnList = "variant_sku"),
    @Index(name = "idx_order_items_unique", columnList = "order_id,variant_sku")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_order_items", columnNames = {"order_id", "variant_sku"})
})
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Product variant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_sku", nullable = false)
    private ProductVariant variant;

    @NotBlank(message = "Product name snapshot is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName; // Snapshot of product name at checkout

    @Size(max = 255, message = "Variant name must not exceed 255 characters")
    @Column(name = "variant_name", length = 255)
    private String variantName; // Snapshot of variant info (color, size, etc.) at checkout

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Price snapshot is required")
    @Min(value = 0, message = "Price snapshot must be non-negative")
    @Column(nullable = false)
    private Long price; // Price snapshot at purchase time
}
