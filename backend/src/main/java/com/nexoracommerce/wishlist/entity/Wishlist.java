package com.nexoracommerce.wishlist.entity;

import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.user.entity.User;
import jakarta.persistence.*;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "product"})
@Entity
@Table(name = "wishlists", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
}, indexes = {
    @Index(name = "idx_wishlists_user", columnList = "user_id"),
    @Index(name = "idx_wishlists_product", columnList = "product_id")
})
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "wishlists_id_seq")
    @SequenceGenerator(name = "wishlists_id_seq", sequenceName = "wishlists_id_seq", allocationSize = 1)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
