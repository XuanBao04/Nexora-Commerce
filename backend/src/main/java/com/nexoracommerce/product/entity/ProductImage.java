package com.nexoracommerce.product.entity;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"product", "variant"})
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_images_product", columnList = "product_id"),
    @Index(name = "idx_product_images_sku", columnList = "sku")
})
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_images_id_seq")
    @SequenceGenerator(name = "product_images_id_seq", sequenceName = "product_images_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku")
    private ProductVariant variant;

    @NotBlank(message = "Image URL is required")
    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @NotNull(message = "Primary flag is required")
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
}
