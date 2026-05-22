package com.shopcart.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Product {
    @Id
    @Column(name = "id", length = 50)
    private String id;

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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @Transient
    private com.shopcart.common.enums.ProductStatus status;

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
