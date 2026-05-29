package com.nexoracommerce.order.repository;

import com.nexoracommerce.common.repository.BaseRepository;
import com.nexoracommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public interface OrderItemRepository extends BaseRepository<OrderItem, Long> {

    /**
     * Find all items in an order
     */
    List<OrderItem> findByOrder_Id(String orderId);

    /**
     * Find order items by order ID with variant details
     */
    @Query("""
            select distinct oi
            from OrderItem oi
            left join fetch oi.variant
            where oi.order.id = :orderId
            """)
    List<OrderItem> findByOrderIdWithVariant(@Param("orderId") String orderId);

    /**
     * Find all order items for a specific variant
     */
    List<OrderItem> findByVariant_Sku(String variantSku);

    /**
     * Find all order items for a product
     */
    @Query("""
            select oi
            from OrderItem oi
            where oi.variant.product.id = :productId
            order by oi.id desc
            """)
    List<OrderItem> findByProductId(@Param("productId") String productId);

    /**
     * Count items in an order
     */
    long countByOrder_Id(String orderId);

    /**
     * Sum total quantity for a variant across all orders
     */
    @Query("select coalesce(sum(oi.quantity), 0) from OrderItem oi where oi.variant.sku = :variantSku")
    long getTotalQuantitySoldByVariant(@Param("variantSku") String variantSku);

    /**
     * Sum total revenue for a specific variant
     */
    @Query("""
            select coalesce(sum(oi.quantity * oi.price), 0)
            from OrderItem oi
            where oi.variant.sku = :variantSku
            """)
    long getTotalRevenueByVariant(@Param("variantSku") String variantSku);

    /**
     * Sum total revenue for a product
     */
    @Query("""
            select coalesce(sum(oi.quantity * oi.price), 0)
            from OrderItem oi
            where oi.variant.product.id = :productId
            """)
    long getTotalRevenueByProduct(@Param("productId") String productId);

    /**
     * Check if variant exists in any order
     */
    boolean existsByVariant_Sku(String variantSku);

    /**
     * Check if variant exists in a specific order
     */
    boolean existsByOrder_IdAndVariant_Sku(String orderId, String variantSku);
}
