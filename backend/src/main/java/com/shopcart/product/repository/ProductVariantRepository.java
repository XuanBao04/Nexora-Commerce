package com.shopcart.product.repository;

import com.shopcart.common.repository.BaseRepository;
import com.shopcart.product.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends BaseRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySku(String sku);

    List<ProductVariant> findBySkuIn(Collection<String> skus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.sku = :sku")
    Optional<ProductVariant> findBySkuWithLock(@Param("sku") String sku);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.quantity = pv.quantity - :qty,
            pv.soldQuantity = COALESCE(pv.soldQuantity, 0) + :qty,
            pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) - :qty
        WHERE pv.sku = :sku
        AND COALESCE(pv.reservedQuantity, 0) >= :qty
    """)
    int deductStockOnCompletion(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) + :qty
        WHERE pv.sku = :sku
        AND (pv.quantity - COALESCE(pv.reservedQuantity, 0)) >= :qty
    """)
    int reserveStock(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.reservedQuantity = COALESCE(pv.reservedQuantity, 0) - :qty
        WHERE pv.sku = :sku
        AND COALESCE(pv.reservedQuantity, 0) >= :qty
    """)
    int releaseStock(@Param("sku") String sku, @Param("qty") int qty);
}
