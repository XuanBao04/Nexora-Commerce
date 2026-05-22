package com.shopcart.redis.controller;

import com.shopcart.redis.dto.StockStatus;
import com.shopcart.redis.service.RedisStockService;
import com.shopcart.redis.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for Redis stock management
 * These endpoints should only be accessible by ADMIN users
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final RedisStockService redisStockService;
    private final StockSyncService stockSyncService;
    
    /**
     * Get current Redis stock for a product
     * GET /api/admin/stock/{productId}
     */
    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockStatus> getProductStock(@PathVariable String productId) {
        long stock = redisStockService.getStock(productId);
        return ResponseEntity.ok(
                StockStatus.builder()
                        .productId(productId)
                        .redisStock(stock)
                        .locked(redisStockService.isLocked(productId))
                        .build()
        );
    }
    
    /**
     * Manually set Redis stock for a product (emergency use only)
     * PUT /api/admin/stock/{productId}
     * Body: { "quantity": 100 }
     */
    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> setProductStock(
            @PathVariable String productId,
            @RequestParam long quantity) {
        
        log.warn("Manually setting stock: productId={}, quantity={}", productId, quantity);
        redisStockService.setStock(productId, quantity);
        return ResponseEntity.ok("Stock set to " + quantity + " for product " + productId);
    }
    
    /**
     * Sync a single product's stock from DB to Redis
     * POST /api/admin/stock/{productId}/sync
     */
    @PostMapping("/{productId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncProductStock(@PathVariable String productId) {
        log.info("Syncing product stock: productId={}", productId);
        stockSyncService.syncProductStock(productId);
        return ResponseEntity.ok("Stock synced for product: " + productId);
    }
    
    /**
     * Sync all products' stock from DB to Redis
     * POST /api/admin/stock/sync-all
     */
    @PostMapping("/sync-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> syncAllStock() {
        log.info("Syncing all stock from DB to Redis");
        stockSyncService.syncAllStock();
        return ResponseEntity.ok("All stock synced from DB to Redis");
    }
    
    /**
     * Clear all Redis stock data (emergency reset only)
     * DELETE /api/admin/stock/clear-all
     */
    @DeleteMapping("/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearAllStock() {
        log.warn("Clearing all Redis stock data");
        redisStockService.clearAllStock();
        return ResponseEntity.ok("All Redis stock data cleared");
    }
    
    /**
     * Delete stock for a specific product
     * DELETE /api/admin/stock/{productId}
     */
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteProductStock(@PathVariable String productId) {
        log.warn("Deleting stock for product: productId={}", productId);
        redisStockService.deleteStock(productId);
        return ResponseEntity.ok("Stock deleted for product: " + productId);
    }
}
