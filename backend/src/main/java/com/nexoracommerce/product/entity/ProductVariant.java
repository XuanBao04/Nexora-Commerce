package com.nexoracommerce.product.entity;

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

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "sku")
@ToString(exclude = {"product", "attributeValues"})
@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_variants_product", columnList = "product_id")
})
public class ProductVariant {
    @Id
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    @Column(name = "sku", length = 50)
    private String sku;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be non-negative")
    @Column(nullable = false)
    private Long price;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0; // Total stock

    @NotNull(message = "Reserved quantity is required")
    @Min(value = 0, message = "Reserved quantity must be non-negative")
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0; // Hold stock for checkout

    @NotNull(message = "Sold quantity is required")
    @Min(value = 0, message = "Sold quantity must be non-negative")
    @Column(name = "sold_quantity", nullable = false)
    @Builder.Default
    private Integer soldQuantity = 0; // Actually sold count

    @Column(name = "original_price")
    @Min(value = 0, message = "Original price must be non-negative")
    private Long originalPrice; // Original price for discount display

    @Column(nullable = false)
    @Builder.Default
    private Integer weight = 0; // Trọng lượng (gram)

    @Column(nullable = false)
    @Builder.Default
    private Integer length = 0; // Kích thước (cm)

    @Column(nullable = false)
    @Builder.Default
    private Integer width = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer height = 0;

    @Column(name = "embedding", columnDefinition = "vector(768)", insertable = false, updatable = false)
    private String embedding; // Semantic Search vector — read-only for JPA, writes via native query only

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "variant_attribute_values",
        joinColumns = @JoinColumn(name = "sku"),
        inverseJoinColumns = @JoinColumn(name = "value_id")
    )
    @BatchSize(size = 20)
    @Builder.Default
    private Set<ProductAttributeValue> attributeValues = new HashSet<>();
}
