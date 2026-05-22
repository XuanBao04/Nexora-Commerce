package com.shopcart.cart.service.impl;

import com.shopcart.constant.MessageConstant;

import com.shopcart.cart.dto.request.CartItemRequest;
import com.shopcart.cart.dto.response.CartResponse;
import com.shopcart.common.exception.BusinessLogicException;
import com.shopcart.common.exception.ResourceNotFoundException;
import com.shopcart.cart.mapper.CartMapper;
import com.shopcart.cart.service.ICartService;
import com.shopcart.inventory.service.IInventoryService;
import com.shopcart.product.entity.Product;
import com.shopcart.product.repository.ProductRepository;
import com.shopcart.product.service.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
public class CartServiceImpl implements ICartService {

    private final RedisCartService redisCartService;
    private final CartMapper cartMapper;
    private final IProductService productService;
    private final IInventoryService inventoryService;
    private final ProductRepository productRepository;

    @Override
    public CartResponse getCart(String userId) {
        return buildCartResponse(userId);
    }

    @Override
    public CartResponse addToCart(String userId, CartItemRequest request) {
        // Kiểm tra sản phẩm có tồn tại không
        productService.getProductById(request.getProductId());

        // Reserve stock trước khi cập nhật giỏ hàng (sẽ ném exception nếu không đủ hàng)
        inventoryService.reserveStock(request.getProductId(), request.getQuantity());

        // Thêm vào Redis (HINCRBY — tự tăng nếu đã tồn tại)
        redisCartService.addItem(userId, request.getProductId(), request.getQuantity());

        return buildCartResponse(userId);
    }

    @Override
    public CartResponse removeFromCart(String userId, String productId) {
        // Kiểm tra item có tồn tại trong cart Redis không
        Integer currentQty = redisCartService.getItemQuantity(userId, productId);
        if (currentQty == null) {
            throw new ResourceNotFoundException(
                    MessageConstant.Cart.NOT_FOUND + productId);
        }

        // Giải phóng kho khi xóa khỏi giỏ
        inventoryService.releaseStock(productId, currentQty);

        redisCartService.removeItem(userId, productId);
        return buildCartResponse(userId);
    }

    @Override
    public CartResponse updateQuantity(String userId, String productId, Integer quantity) {
        // Kiểm tra item có tồn tại trong cart Redis không
        Integer oldQuantity = redisCartService.getItemQuantity(userId, productId);
        if (oldQuantity == null) {
            throw new ResourceNotFoundException(
                    MessageConstant.Cart.NOT_FOUND + productId);
        }

        int diff = quantity - oldQuantity;

        // Điều chỉnh số lượng giữ trong kho
        if (diff > 0) {
            inventoryService.reserveStock(productId, diff);
        } else if (diff < 0) {
            inventoryService.releaseStock(productId, Math.abs(diff));
        }

        redisCartService.setItemQuantity(userId, productId, quantity);
        return buildCartResponse(userId);
    }

    @Override
    public void clearCart(String userId) {
        // Giải phóng kho cho tất cả các mặt hàng trong giỏ trước khi xóa
        Map<String, Integer> cart = redisCartService.getCart(userId);
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            inventoryService.releaseStock(entry.getKey(), entry.getValue());
        }
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

        List<String> productIds = new ArrayList<>(cart.keySet());

        Map<String, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        return cartMapper.toCartResponseFromRedis(userId, cart, productsById);
    }
}
