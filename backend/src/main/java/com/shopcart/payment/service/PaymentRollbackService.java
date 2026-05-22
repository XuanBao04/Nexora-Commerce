package com.shopcart.payment.service;

import com.shopcart.inventory.service.IInventoryService;
import com.shopcart.order.entity.Order;
import com.shopcart.order.entity.OrderItem;
import com.shopcart.product.entity.ProductVariant;
import com.shopcart.product.repository.ProductVariantRepository;
import com.shopcart.redis.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle payment failure scenarios
 * Restores stock in both Redis and Database
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRollbackService {

    private final IInventoryService inventoryService;
    private final RedisStockService redisStockService;
    private final ProductVariantRepository productVariantRepository;
    
    /**
     * Rollbacks order payment:
     * 1. Releases reserved stock back to available pool in DB
     * 2. Increments stock counter in Redis
     */
    @Transactional
    public void rollbackPaymentFailure(Order order) {
        log.warn("Rolling back payment failure for order: orderId={}, userId={}", 
                order.getId(), order.getUser() != null ? order.getUser().getId().toString() : null);
        
        try {
            for (OrderItem item : order.getOrderItems()) {
                rollbackOrderItemPayment(item.getProductId(), item.getQuantity());
            }
            log.info("Payment rollback completed for order: orderId={}", order.getId());
        } catch (Exception e) {
            log.error("Error during payment rollback for order: orderId={}", order.getId(), e);
            // Log but don't throw - we need manual intervention for this case
        }
    }
    
    /**
     * Rollbacks a single order item payment
     */
    private void rollbackOrderItemPayment(String productId, Integer quantity) {
        try {
            // 1. Release reserved stock in DB (back to available pool)
            inventoryService.releaseStock(productId, quantity);
            log.info("Released reserved stock in DB: productId={}, quantity={}", 
                    productId, quantity);
            
            // 2. Increment stock in Redis
            redisStockService.incrementStock(productId, quantity);
            log.info("Incremented stock in Redis: productId={}, quantity={}", 
                    productId, quantity);
            
        } catch (Exception e) {
            log.error("Error rolling back order item: productId={}, quantity={}", 
                    productId, quantity, e);
            throw new RuntimeException("Payment rollback failed for product: " + productId, e);
        }
    }
    
    /**
     * Manually restore stock if rollback failed or needs to be retried
     * Use this for manual fixing of stock inconsistencies
     */
    @Transactional
    public void manuallyRestoreStock(String productId, Integer quantity) {
        log.warn("Manually restoring stock: productId={}, quantity={}", productId, quantity);
        
        try {
            // Restore in DB
            ProductVariant variant = productVariantRepository.findBySku(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + productId));
            
            int currentReserved = variant.getReservedQuantity() != null ? 
                    variant.getReservedQuantity() : 0;
            int newReserved = Math.max(0, currentReserved - quantity);
            
            variant.setReservedQuantity(newReserved);
            productVariantRepository.save(variant);
            
            // Restore in Redis
            redisStockService.incrementStock(productId, quantity);
            
            log.info("Stock manually restored: productId={}, quantity={}", productId, quantity);
        } catch (Exception e) {
            log.error("Error manually restoring stock: productId={}", productId, e);
            throw e;
        }
    }
}
