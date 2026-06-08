package com.nexoracommerce.payment.service;

import com.nexoracommerce.inventory.service.InventoryService;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.redis.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRollbackService {

    private final InventoryService inventoryService;
    private final RedisStockService redisStockService;
    private final ProductVariantRepository productVariantRepository;

    // Hoàn trả tồn kho DB + Redis khi thanh toán thất bại
    @Transactional
    public void rollbackPaymentFailure(Order order) {
        log.warn("Rolling back payment: orderId={}", order.getId());

        try {
            for (OrderItem item : order.getOrderItems()) {
                rollbackOrderItem(item.getVariant().getSku(), item.getQuantity());
            }
            log.info("Payment rollback completed: orderId={}", order.getId());
        } catch (Exception e) {
            log.error("Error during payment rollback: orderId={}", order.getId(), e);
        }
    }

    // Khôi phục tồn kho thủ công (dùng khi rollback tự động thất bại)
    @Transactional
    public void manuallyRestoreStock(String productId, Integer quantity) {
        log.warn("Manually restoring stock: productId={}, quantity={}", productId, quantity);

        ProductVariant variant = productVariantRepository.findBySku(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product variant not found: " + productId));

        int currentReserved = variant.getReservedQuantity() != null ? variant.getReservedQuantity() : 0;
        variant.setReservedQuantity(Math.max(0, currentReserved - quantity));
        productVariantRepository.save(variant);

        redisStockService.incrementStock(productId, quantity);
        log.info("Stock manually restored: productId={}, quantity={}", productId, quantity);
    }

    private void rollbackOrderItem(String variantSku, Integer quantity) {
        try {
            inventoryService.releaseStock(variantSku, quantity);
            redisStockService.incrementStock(variantSku, quantity);
        } catch (Exception e) {
            log.error("Error rolling back item: sku={}, qty={}", variantSku, quantity, e);
            throw new RuntimeException("Payment rollback failed for variant: " + variantSku, e);
        }
    }
}
