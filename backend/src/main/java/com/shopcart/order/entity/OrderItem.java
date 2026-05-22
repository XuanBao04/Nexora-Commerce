package com.shopcart.order.entity;

import com.shopcart.product.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_items_id_seq")
    @SequenceGenerator(name = "order_items_id_seq", sequenceName = "order_items_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_sku", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Long price; // Price snapshot at purchase time

    @Transient
    private String productId;

    @Transient
    public String getProductId() {
        return variant != null ? variant.getSku() : null;
    }
}
