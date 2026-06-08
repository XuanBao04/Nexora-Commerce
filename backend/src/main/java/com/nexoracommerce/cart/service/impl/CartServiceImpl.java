package com.nexoracommerce.cart.service.impl;

import com.nexoracommerce.cart.dto.request.CartItemRequest;
import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.mapper.CartMapper;
import com.nexoracommerce.cart.service.CartService;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.inventory.service.InventoryService;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final RedisCartService redisCartService;
    private final CartMapper cartMapper;
    private final InventoryService inventoryService;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public CartResponse getCart(String userId) {
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String userId, CartItemRequest request) {
        // Kiểm tra variant tồn tại
        ProductVariant variant = productVariantRepository.findBySku(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageConstant.Product.NOT_FOUND + request.productId()));

        // Kiểm tra tồn kho khả dụng
        Integer currentInCart = redisCartService.getItemQuantity(userId, variant.getSku());
        int targetQuantity = (currentInCart != null ? currentInCart : 0) + request.quantity();

        if (!inventoryService.hasEnoughStock(variant.getSku(), targetQuantity)) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK + variant.getSku());
        }

        redisCartService.addItem(userId, variant.getSku(), request.quantity());
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(String userId, String productId) {
        Integer currentQty = redisCartService.getItemQuantity(userId, productId);
        if (currentQty == null) {
            throw new ResourceNotFoundException(MessageConstant.Cart.NOT_FOUND + productId);
        }

        redisCartService.removeItem(userId, productId);
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(String userId, String productId, Integer quantity) {
        Integer oldQuantity = redisCartService.getItemQuantity(userId, productId);
        if (oldQuantity == null) {
            throw new ResourceNotFoundException(MessageConstant.Cart.NOT_FOUND + productId);
        }

        if (!inventoryService.hasEnoughStock(productId, quantity)) {
            throw new BusinessLogicException(MessageConstant.Inventory.INSUFFICIENT_STOCK + productId);
        }

        redisCartService.setItemQuantity(userId, productId, quantity);
        return buildCartResponse(userId);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        redisCartService.clearCart(userId);
    }

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
