package com.nexoracommerce.inventory.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.inventory.mapper.InventoryMapper;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse getInventoryDetails(String productId) {
        validateProductId(productId);
        return inventoryMapper.toResponse(findVariantOrThrow(productId));
    }

    @Override
    public Integer getStock(String productId) {
        validateProductId(productId);
        return calculateAvailableStock(findVariantOrThrow(productId));
    }

    @Override
    public boolean hasEnoughStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        ProductVariant variant = productVariantRepository.findBySku(productId).orElse(null);
        if (variant == null) return false;
        return calculateAvailableStock(variant) >= quantity;
    }

    @Override
    @Transactional
    public void updateStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        ProductVariant variant = findVariantOrThrow(productId);
        int newAvailable = quantity - safeReserved(variant);
        if (newAvailable < 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK + productId);
        }

        variant.setQuantity(quantity);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void reserveStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        int updated = productVariantRepository.reserveStock(productId, quantity);
        if (updated == 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK_RESERVE + productId);
        }
    }

    @Override
    @Transactional
    public void releaseStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        ProductVariant variant = findVariantOrThrow(productId);
        int newReserved = Math.max(0, safeReserved(variant) - quantity);
        variant.setReservedQuantity(newReserved);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void confirmStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);
        log.info("Stock reservation confirmed: productId={}, quantity={}", productId, quantity);
    }

    @Override
    @Transactional
    public void shipStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        int updated = productVariantRepository.deductStockOnCompletion(productId, quantity);
        if (updated == 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK_RESERVE + productId);
        }
    }

    private ProductVariant findVariantOrThrow(String productId) {
        return productVariantRepository.findBySku(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Inventory.NOT_FOUND + productId));
    }

    private int calculateAvailableStock(ProductVariant variant) {
        return variant.getQuantity() - safeReserved(variant);
    }

    private int safeReserved(ProductVariant variant) {
        return variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessLogicException(MessageConstant.Product.ID_REQUIRED);
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.POSITIVE_QUANTITY);
        }
    }
}
