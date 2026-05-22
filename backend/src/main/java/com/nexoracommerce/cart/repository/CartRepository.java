package com.nexoracommerce.cart.repository;
import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.cart.entity.CartItem;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends BaseRepository<CartItem, Long> {
    List<CartItem> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    Optional<CartItem> findByUser_IdAndVariant_Sku(UUID userId, String sku);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    @org.springframework.transaction.annotation.Transactional
    void deleteByUser_Id(UUID userId);
}