package com.nexoracommerce.cart.mapper;

import com.nexoracommerce.cart.dto.response.CartResponse;
import com.nexoracommerce.cart.dto.response.CartItemResponse;
import com.nexoracommerce.cart.entity.CartItem;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    default CartResponse toCartResponse(String userId, List<CartItem> items, Map<String, Product> productsById) {
        if (items == null) {
            items = java.util.Collections.emptyList();
        }

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> toCartItemResponse(item, productsById))
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

    default CartItemResponse toCartItemResponse(CartItem item, Map<String, Product> productsById) {
        Product product = productsById.get(item.getProductId());
        if (product == null) {
            throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + item.getProductId());
        }

        Long price = product.getPrice();
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

    default CartResponse toCartResponseFromRedis(String userId, Map<String, Integer> redisCart, Map<String, Product> productsById) {
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
                    Product product = productsById.get(productId);
                    if (product == null) {
                        throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + productId);
                    }

                    Long price = product.getPrice();
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
