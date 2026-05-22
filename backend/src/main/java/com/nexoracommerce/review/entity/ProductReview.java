package com.nexoracommerce.review.entity;

import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "variant", "order", "parent", "replies", "images"})
@Entity
@Table(name = "product_reviews", indexes = {
    @Index(name = "idx_reviews_user", columnList = "user_id"),
    @Index(name = "idx_reviews_variant", columnList = "variant_sku"),
    @Index(name = "idx_reviews_order", columnList = "order_id"),
    @Index(name = "idx_reviews_parent", columnList = "parent_id")
})
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_reviews_id_seq")
    @SequenceGenerator(name = "product_reviews_id_seq", sequenceName = "product_reviews_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Product variant is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_sku", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order; // Nullable, set for verified purchase badge

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @Column(nullable = false)
    private Integer rating; // 1 to 5 stars

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ProductReview parent; // For Admin or CSKH replies

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductReview> replies = new ArrayList<>();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ReviewImage> images = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}

