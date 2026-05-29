package com.nexoracommerce.product.entity;

import com.nexoracommerce.brand.entity.Brand;
import com.nexoracommerce.category.entity.Category;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"category", "brand", "variants", "images"})
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_category", columnList = "category_id"),
    @Index(name = "idx_products_brand", columnList = "brand_id")
})
public class Product {
    @Id
    @NotBlank(message = "Product ID is required")
    @Size(max = 50, message = "Product ID must not exceed 50 characters")
    @Column(name = "id", length = 50)
    private String id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @MapKey(name = "sku")
    @BatchSize(size = 20)
    @Builder.Default
    private Map<String, ProductVariant> variants = new LinkedHashMap<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<ProductImage> images = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.nexoracommerce.common.enums.ProductStatus status;

    @Transient
    private String imagePublicId;

    public Long getPrice() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        ProductVariant variant = variants.get(this.id);
        if (variant != null) {
            return variant.getPrice();
        }
        // Fallback: get first variant if SKU-matched variant not found
        return variants.values().stream()
                .findFirst()
                .map(ProductVariant::getPrice)
                .orElse(null);
    }

    public void setPrice(Long price) {
        if (price == null) return;
        if (variants == null) {
            variants = new LinkedHashMap<>();
        }
        ProductVariant variant = variants.get(this.id);
        if (variant != null) {
            variant.setPrice(price);
        } else {
            ProductVariant newVariant = ProductVariant.builder()
                    .sku(this.id)
                    .product(this)
                    .price(price)
                    .quantity(0)
                    .reservedQuantity(0)
                    .soldQuantity(0)
                    .build();
            variants.put(this.id, newVariant);
        }
    }

    public Integer getQuantity() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        ProductVariant variant = variants.get(this.id);
        if (variant != null) {
            return variant.getQuantity();
        }
        // Fallback: get first variant if SKU-matched variant not found
        return variants.values().stream()
                .findFirst()
                .map(ProductVariant::getQuantity)
                .orElse(null);
    }

    public String getImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElseGet(() -> images.stream()
                        .findAny()
                        .map(ProductImage::getImageUrl)
                        .orElse(null));
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl == null) return;
        if (images == null) {
            images = new HashSet<>();
        }
        images.stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .ifPresentOrElse(
                    img -> img.setImageUrl(imageUrl),
                    () -> {
                        ProductImage img = ProductImage.builder()
                                .product(this)
                                .imageUrl(imageUrl)
                                .isPrimary(true)
                                .build();
                        images.add(img);
                    }
                );
    }
}
