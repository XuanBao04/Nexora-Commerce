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

// Đồng bộ tồn kho từ PostgreSQL sang Redis
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncServiceImpl implements StockSyncService {

    private final RedisStockService redisStockService;
    private final ProductVariantRepository productVariantRepository;

    // Đồng bộ khi khởi động ứng dụng
    @EventListener(ApplicationReadyEvent.class)
    public void syncStockOnStartup() {
        log.info("Starting initial stock sync from DB to Redis...");
        try {
            syncAllStock();
            log.info("Initial stock sync completed");
        } catch (Exception e) {
            log.error("Error during initial stock sync", e);
        }
    }

    // Đồng bộ định kỳ mỗi 5 phút
    @Scheduled(fixedDelay = 300000)
    public void syncStockPeriodically() {
        log.debug("Periodic stock sync triggered");
        try {
            syncAllStock();
        } catch (Exception e) {
            log.error("Error during periodic stock sync", e);
        }
    }

    public void syncProductStock(String productId) {
        try {
            ProductVariant variant = productVariantRepository.findBySku(productId).orElse(null);

            if (variant != null) {
                long availableStock = calculateAvailableStock(variant);
                redisStockService.setStock(productId, availableStock);
            } else {
                log.warn("Product variant not found for sync: productId={}", productId);
                redisStockService.setStock(productId, 0);
            }
        } catch (Exception e) {
            log.error("Error syncing product stock: productId={}", productId, e);
        }
    }

    public void syncAllStock() {
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        log.info("Syncing {} variants from DB to Redis", allVariants.size());

        for (ProductVariant variant : allVariants) {
            long availableStock = calculateAvailableStock(variant);
            redisStockService.setStock(variant.getSku(), availableStock);
        }

        log.info("Stock sync completed for {} variants", allVariants.size());
    }

    private long calculateAvailableStock(ProductVariant variant) {
        int total = variant.getQuantity() != null ? variant.getQuantity() : 0;
        int reserved = variant.getReservedQuantity() != null ? variant.getReservedQuantity() : 0;
        return Math.max(0, total - reserved);
    }
}
