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
        SELECT DISTINCT oi 
        FROM OrderItem oi 
        LEFT JOIN FETCH oi.variant 
        WHERE oi.order.id = :orderId
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
        SELECT oi 
        FROM OrderItem oi 
        WHERE oi.variant.product.id = :productId 
        ORDER BY oi.id DESC
    """)
    List<OrderItem> findByProductId(@Param("productId") String productId);

    /**
     * Count items in an order
     */
    long countByOrder_Id(String orderId);

    /**
     * Sum total quantity for a variant across all orders
     */
    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0) 
        FROM OrderItem oi 
        WHERE oi.variant.sku = :variantSku
    """)
    long getTotalQuantitySoldByVariant(@Param("variantSku") String variantSku);

    /**
     * Sum total revenue for a specific variant
     */
    @Query("""
        SELECT COALESCE(SUM(oi.quantity * oi.price), 0) 
        FROM OrderItem oi 
        WHERE oi.variant.sku = :variantSku
    """)
    long getTotalRevenueByVariant(@Param("variantSku") String variantSku);

    /**
     * Sum total revenue for a product
     */
    @Query("""
        SELECT COALESCE(SUM(oi.quantity * oi.price), 0) 
        FROM OrderItem oi 
        WHERE oi.variant.product.id = :productId
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

    /**
     * Find best selling products aggregated completely at database-level.
     */
    @Query("""
        SELECT new com.nexoracommerce.statistic.dto.response.BestSellerItem(
            oi.productName, 
            oi.variant.sku, 
            CAST(SUM(oi.quantity) AS int), 
            SUM(oi.quantity * oi.price)
        ) 
        FROM OrderItem oi 
        WHERE oi.order.paymentStatus = 'PAID' 
        GROUP BY oi.productName, oi.variant.sku 
        ORDER BY SUM(oi.quantity) DESC
    """)
    List<com.nexoracommerce.statistic.dto.response.BestSellerItem> findBestSellers(org.springframework.data.domain.Pageable pageable);
}
