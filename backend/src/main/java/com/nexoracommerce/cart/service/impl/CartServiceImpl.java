package com.nexoracommerce.cart.service.impl;

import com.nexoracommerce.constant.MessageConstant;

import com.nexoracommerce.cart.dto.request.CartItemRequest;
import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.cart.mapper.CartMapper;
import com.nexoracommerce.cart.service.ICartService;
import com.nexoracommerce.inventory.service.IInventoryService;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý các thao tác liên quan đến giỏ hàng (Cart).
 * Sử dụng Redis Hash để lưu trữ giỏ hàng thay vì PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements ICartService {

    private final RedisCartService redisCartService;
    private final CartMapper cartMapper;
    private final IInventoryService inventoryService;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public CartResponse getCart(String userId) {
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String userId, CartItemRequest request) {
        // Kiểm tra variant có tồn tại không (cart dùng SKU)
        ProductVariant variant = productVariantRepository.findBySku(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstant.Product.NOT_FOUND + request.productId()));

        // Kiểm tra tồn kho khả dụng trước khi thêm vào giỏ (không gọi reserveStock ở CSDL)
        Integer currentInCart = redisCartService.getItemQuantity(userId, variant.getSku());
        int targetQuantity = (currentInCart != null ? currentInCart : 0) + request.quantity();

        boolean hasStock = inventoryService.hasEnoughStock(variant.getSku(), targetQuantity);
        if (!hasStock) {
            throw new BusinessLogicException(
                    MessageConstant.Inventory.INSUFFICIENT_STOCK + variant.getSku());
        }

        // Thêm vào Redis (HINCRBY — tự tăng nếu đã tồn tại)
        redisCartService.addItem(userId, variant.getSku(), request.quantity());

        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(String userId, String productId) {
        // Kiểm tra item có tồn tại trong cart Redis không
        Integer currentQty = redisCartService.getItemQuantity(userId, productId);
        if (currentQty == null) {
            throw new ResourceNotFoundException(
                    MessageConstant.Cart.NOT_FOUND + productId);
        }

        redisCartService.removeItem(userId, productId);
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(String userId, String productId, Integer quantity) {
        // Kiểm tra item có tồn tại trong cart Redis không
        Integer oldQuantity = redisCartService.getItemQuantity(userId, productId);
        if (oldQuantity == null) {
            throw new ResourceNotFoundException(
                    MessageConstant.Cart.NOT_FOUND + productId);
        }

        // Kiểm tra tồn kho khả dụng cho số lượng mới cập nhật
        boolean hasStock = inventoryService.hasEnoughStock(productId, quantity);
        if (!hasStock) {
            throw new BusinessLogicException(
                    MessageConstant.Inventory.INSUFFICIENT_STOCK + productId);
        }

        redisCartService.setItemQuantity(userId, productId, quantity);
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        redisCartService.clearCart(userId);
    }

    // ======================== Private Helper Methods ========================

    /**
     * Lấy toàn bộ cart items từ Redis và build thành CartResponse.
     */
    private CartResponse buildCartResponse(String userId) {
        Map<String, Integer> cart = redisCartService.getCart(userId);

        if (cart.isEmpty()) {
            return cartMapper.toCartResponseFromRedis(userId, cart, Map.of());
        }

        List<String> skus = new ArrayList<>(cart.keySet());

        Map<String, ProductVariant> variantsBySku = productVariantRepository.findAllById(skus).stream()
                .collect(Collectors.toMap(ProductVariant::getSku, variant -> variant));

        return cartMapper.toCartResponseFromRedis(userId, cart, variantsBySku);
    }
}
