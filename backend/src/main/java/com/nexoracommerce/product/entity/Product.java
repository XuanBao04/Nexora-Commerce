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

import java.util.ArrayList;
import java.util.List;

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
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Transient
    private com.nexoracommerce.common.enums.ProductStatus status;

    @Transient
    private String imagePublicId;

    public Long getPrice() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(v -> v.getSku().equals(this.id))
                .findFirst()
                .map(ProductVariant::getPrice)
                .orElse(variants.get(0).getPrice());
    }

    public void setPrice(Long price) {
        if (price == null) return;
        if (variants == null) {
            variants = new ArrayList<>();
        }
        variants.stream()
                .filter(v -> v.getSku().equals(this.id))
                .findFirst()
                .ifPresentOrElse(
                    v -> v.setPrice(price),
                    () -> {
                        ProductVariant v = ProductVariant.builder()
                                .sku(this.id)
                                .product(this)
                                .price(price)
                                .quantity(0)
                                .reservedQuantity(0)
                                .soldQuantity(0)
                                .build();
                        variants.add(v);
                    }
                );
    }

    public Integer getQuantity() {
        if (variants == null || variants.isEmpty()) {
            return null;
        }
        return variants.stream()
                .filter(v -> v.getSku().equals(this.id))
                .findFirst()
                .map(ProductVariant::getQuantity)
                .orElse(variants.get(0).getQuantity());
    }

    public String getImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::getIsPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(images.get(0).getImageUrl());
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl == null) return;
        if (images == null) {
            images = new ArrayList<>();
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
