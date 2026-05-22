package com.nexoracommerce.redis.service;

import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service to sync stock from PostgreSQL database to Redis cache
 * Ensures Redis has latest stock data on startup and periodically
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

    private final RedisStockService redisStockService;
    private final ProductVariantRepository productVariantRepository;
    
    /**
     * Syncs all product stock from DB to Redis on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncStockOnStartup() {
        log.info("Starting initial stock sync from DB to Redis...");
        try {
            syncAllStock();
            log.info("Initial stock sync completed successfully");
        } catch (Exception e) {
            log.error("Error during initial stock sync", e);
            // Don't fail startup, but log error
        }
    }
    
    /**
     * Periodically syncs stock from DB to Redis (every 5 minutes)
     * This ensures Redis stays in sync even if there are inconsistencies
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void syncStockPeriodically() {
        log.debug("Periodic stock sync from DB to Redis triggered");
        try {
            syncAllStock();
        } catch (Exception e) {
            log.error("Error during periodic stock sync", e);
        }
    }
    
    /**
     * Syncs a single product's stock from DB to Redis
     */
    public void syncProductStock(String productId) {
        try {
            ProductVariant variant = productVariantRepository.findBySku(productId)
                    .orElse(null);
            
            if (variant != null) {
                long availableStock = calculateAvailableStock(variant);
                redisStockService.setStock(productId, availableStock);
                log.info("Synced product stock: productId={}, availableStock={}", 
                        productId, availableStock);
            } else {
                log.warn("Product variant not found for sync: productId={}", productId);
                redisStockService.setStock(productId, 0);
            }
        } catch (Exception e) {
            log.error("Error syncing product stock: productId={}", productId, e);
        }
    }
    
    /**
     * Syncs all products' stock from DB to Redis
     */
    public void syncAllStock() {
        try {
            List<ProductVariant> allVariants = productVariantRepository.findAll();
            log.info("Syncing {} variants from DB to Redis", allVariants.size());
            
            for (ProductVariant variant : allVariants) {
                long availableStock = calculateAvailableStock(variant);
                // Under default SKU strategy, variant.getSku() is the productId used by the frontend
                redisStockService.setStock(variant.getSku(), availableStock);
            }
            
            log.info("Stock sync completed for {} variants", allVariants.size());
        } catch (Exception e) {
            log.error("Error syncing all stock from DB to Redis", e);
            throw e;
        }
    }
    
    /**
     * Calculates available stock = total quantity - reserved quantity
     * Matches the logic in InventoryServiceImpl
     */
    private long calculateAvailableStock(ProductVariant variant) {
        int total = variant.getQuantity() != null ? variant.getQuantity() : 0;
        int reserved = variant.getReservedQuantity() != null ? variant.getReservedQuantity() : 0;
        return Math.max(0, total - reserved);
    }
}
