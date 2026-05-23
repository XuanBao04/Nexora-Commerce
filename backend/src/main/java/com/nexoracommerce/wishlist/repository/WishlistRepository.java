package com.nexoracommerce.wishlist.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.wishlist.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public interface WishlistRepository extends BaseRepository<Wishlist, Long> {
    Page<Wishlist> findByUserId(UUID userId, Pageable pageable);
    boolean existsByUserIdAndProductId(UUID userId, String productId);
    Optional<Wishlist> findByUserIdAndProductId(UUID userId, String productId);
}
