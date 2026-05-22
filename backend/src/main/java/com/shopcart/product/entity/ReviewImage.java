package com.shopcart.product.entity;

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
@Table(name = "review_images")
public class ReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "review_images_id_seq")
    @SequenceGenerator(name = "review_images_id_seq", sequenceName = "review_images_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ProductReview review;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;
}
