package com.nexoracommerce.inventory.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.inventory.dto.response.InventoryResponse;
import com.nexoracommerce.inventory.mapper.InventoryMapper;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.inventory.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Inventory management using ProductVariant under the hood
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements IInventoryService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryMapper inventoryMapper;

    private int getSafeReservedQuantity(ProductVariant variant) {
        return variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
    }

    // ======================== Inventory Retrieval ========================
    
    @Override
    public InventoryResponse getInventoryDetails(String productId) {
        validateProductId(productId);
        ProductVariant variant = findVariantOrThrow(productId);
        return inventoryMapper.toResponse(variant);
    }

    @Override
    public Integer getStock(String productId) {
        validateProductId(productId);
        ProductVariant variant = findVariantOrThrow(productId);
        return calculateAvailableStock(variant);
    }

    @Override
    public boolean hasEnoughStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);
        
        ProductVariant variant = productVariantRepository.findBySku(productId).orElse(null);
        if (variant == null) {
            return false;
        }
        return calculateAvailableStock(variant) >= quantity;
    }

    // ======================== Stock Operations (Transactional) ========================

    @Override
    @Transactional
    public void updateStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);
        
        ProductVariant variant = findVariantOrThrow(productId);
        
        int newQuantity = quantity;
        int newAvailable = newQuantity - getSafeReservedQuantity(variant);
        if (newAvailable < 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK + productId);
        }
        
        variant.setQuantity(newQuantity);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void reserveStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        int updated = productVariantRepository.reserveStock(productId, quantity);

        if (updated == 0) {
            throw new BusinessLogicException(
                    MessageConstant.Inventory.INSUFFICIENT_STOCK_RESERVE + productId
            );
        }
    }

    @Override
    @Transactional
    public void releaseStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);
        
        ProductVariant variant = findVariantOrThrow(productId);
        
        int newReserved = getSafeReservedQuantity(variant) - quantity;
        if (newReserved < 0) newReserved = 0;
        variant.setReservedQuantity(newReserved);
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void confirmStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);
        
        ProductVariant variant = findVariantOrThrow(productId);
        
        int newReserved = getSafeReservedQuantity(variant) - quantity;
        if (newReserved < 0) newReserved = 0;
        
        variant.setReservedQuantity(newReserved);
        variant.setQuantity(variant.getQuantity() - quantity);
        variant.setSoldQuantity((variant.getSoldQuantity() == null ? 0 : variant.getSoldQuantity()) + quantity);
        
        productVariantRepository.save(variant);
    }

    @Override
    @Transactional
    public void shipStock(String productId, Integer quantity) {
        validateProductId(productId);
        validateQuantity(quantity);

        int updated = productVariantRepository.deductStockOnCompletion(productId, quantity);

        if (updated == 0) {
            throw new BusinessLogicException(
                    MessageConstant.Inventory.INSUFFICIENT_STOCK_RESERVE + productId
            );
        }
    }

    // ======================== Private Helper Methods ========================

    /**
     * Find variant or throw ResourceNotFoundException
     */
    private ProductVariant findVariantOrThrow(String productId) {
        return productVariantRepository.findBySku(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Inventory.NOT_FOUND + productId));
    }

    /**
     * Calculate available stock = total quantity - reserved
     */
    private int calculateAvailableStock(ProductVariant variant) {
        return variant.getQuantity() - getSafeReservedQuantity(variant);
    }

    /**
     * Validate product ID is not null or empty
     */
    private void validateProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new BusinessLogicException(MessageConstant.Product.ID_REQUIRED);
        }
    }

    /**
     * Validate quantity is positive
     */
    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessLogicException(MessageConstant.Inventory.POSITIVE_QUANTITY);
        }
    }
}
