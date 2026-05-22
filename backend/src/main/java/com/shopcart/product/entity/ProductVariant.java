package com.shopcart.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id
    @Column(name = "sku", length = 50)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0; // Total stock

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0; // Hold stock for checkout

    @Column(name = "sold_quantity", nullable = false)
    @Builder.Default
    private Integer soldQuantity = 0; // Actually sold count

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "variant_attribute_values",
        joinColumns = @JoinColumn(name = "sku"),
        inverseJoinColumns = @JoinColumn(name = "value_id")
    )
    @Builder.Default
    private Set<ProductAttributeValue> attributeValues = new HashSet<>();
}
