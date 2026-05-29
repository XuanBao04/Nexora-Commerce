package com.nexoracommerce.cart.mapper;

import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.dto.response.CartItemResponse;
import com.nexoracommerce.cart.entity.CartItem;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.product.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    default CartResponse toCartResponse(String userId, List<CartItem> items, Map<String, ProductVariant> variantsBySku) {
        if (items == null) {
            items = java.util.Collections.emptyList();
        }

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> toCartItemResponse(item, variantsBySku))
                .collect(Collectors.toList());

        Long totalPrice = itemResponses.stream()
                .mapToLong(CartItemResponse::totalPrice)
                .sum();

        return new CartResponse(
                userId,
                itemResponses,
                items.size(),
                totalPrice
        );
    }

    default CartItemResponse toCartItemResponse(CartItem item, Map<String, ProductVariant> variantsBySku) {
        ProductVariant variant = variantsBySku.get(item.getProductId());
        if (variant == null) {
            throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + item.getProductId());
        }

        Long price = variant.getPrice();
        Long totalPrice = price * item.getQuantity();

        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                price,
                totalPrice,
                item.getCreatedAt()
        );
    }

    default CartResponse toCartResponseFromRedis(String userId, Map<String, Integer> redisCart, Map<String, ProductVariant> variantsBySku) {
        if (redisCart == null || redisCart.isEmpty()) {
            return new CartResponse(
                    userId,
                    java.util.Collections.emptyList(),
                    0,
                    0L
            );
        }

        List<CartItemResponse> itemResponses = redisCart.entrySet().stream()
                .map(entry -> {
                    String productId = entry.getKey();
                    Integer quantity = entry.getValue();
                    ProductVariant variant = variantsBySku.get(productId);
                    if (variant == null) {
                        throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId);
                    }

                    Long price = variant.getPrice();
                    Long totalPrice = price * quantity;

                    return new CartItemResponse(
                            null, // No DB ID in Redis mode
                            productId,
                            quantity,
                            price,
                            totalPrice,
                            null
                    );
                })
                .collect(Collectors.toList());

        Long totalPrice = itemResponses.stream()
                .mapToLong(CartItemResponse::totalPrice)
                .sum();

        return new CartResponse(
                userId,
                itemResponses,
                redisCart.size(),
                totalPrice
        );
    }

    default CartItem toEntity(CartItemResponse response) {
        if (response == null) {
            return null;
        }
        return CartItem.builder()
                .id(response.id())
                .variant(response.productId() != null ? com.nexoracommerce.product.entity.ProductVariant.builder().sku(response.productId()).build() : null)
                .quantity(response.quantity())
                .build();
    }
}
