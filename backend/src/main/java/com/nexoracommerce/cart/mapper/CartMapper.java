package com.nexoracommerce.cart.mapper;

import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.dto.response.CartItemResponse;
import com.nexoracommerce.cart.entity.CartItem;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.product.entity.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class CartMapper {

   
    public CartResponse toCartResponse(String userId, List<CartItem> items, Map<String, Product> productsById) {
        if (items == null) {
            items = java.util.Collections.emptyList();
        }

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> toCartItemResponse(item, productsById))
                .collect(Collectors.toList());

        Long totalPrice = itemResponses.stream()
                .mapToLong(CartItemResponse::getTotalPrice)
                .sum();

        return CartResponse.builder()
                .userId(userId)
                .items(itemResponses)
                .totalItems(items.size())
                .totalPrice(totalPrice)
                .build();
    }

    
    public CartItemResponse toCartItemResponse(CartItem item, Map<String, Product> productsById) {
        Product product = productsById.get(item.getProductId());
        if (product == null) {
            throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + item.getProductId());
        }

        Long price = product.getPrice();
        Long totalPrice = price * item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(price)
                .totalPrice(totalPrice)
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * Build CartResponse from Redis Hash data (productId → quantity).
     */
    public CartResponse toCartResponseFromRedis(String userId, Map<String, Integer> redisCart, Map<String, Product> productsById) {
        if (redisCart == null || redisCart.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .items(java.util.Collections.emptyList())
                    .totalItems(0)
                    .totalPrice(0L)
                    .build();
        }

        List<CartItemResponse> itemResponses = redisCart.entrySet().stream()
                .map(entry -> {
                    String productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    Product product = productsById.get(productId);
                    if (product == null) {
                        throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId);
                    }

                    Long price = product.getPrice();
                    Long totalPrice = price * quantity;

                    return CartItemResponse.builder()
                            .id(null) // No DB ID in Redis mode
                            .productId(productId)
                            .quantity(quantity)
                            .price(price)
                            .totalPrice(totalPrice)
                            .createdAt(null)
                            .build();
                })
                .collect(Collectors.toList());

        Long totalPrice = itemResponses.stream()
                .mapToLong(CartItemResponse::getTotalPrice)
                .sum();

        return CartResponse.builder()
                .userId(userId)
                .items(itemResponses)
                .totalItems(redisCart.size())
                .totalPrice(totalPrice)
                .build();
    }

   
    public CartItem toEntity(CartItemResponse response) {
        return CartItem.builder()
                .id(response.getId())
                .variant(response.getProductId() != null ? com.nexoracommerce.product.entity.ProductVariant.builder().sku(response.getProductId()).build() : null)
                .quantity(response.getQuantity())
                .build();
    }
}

