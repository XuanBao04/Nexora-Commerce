package com.shopcart.order.mapper;

import com.shopcart.order.dto.response.OrderResponse;
import com.shopcart.order.dto.response.OrderItemResponse;
import com.shopcart.order.entity.Order;
import com.shopcart.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getOrderItems() != null
                ? order.getOrderItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList();

        long subtotal = itemResponses.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser() != null ? order.getUser().getId().toString() : null)
                .items(itemResponses)
                .subtotal(subtotal)
                .discountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0L)
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .shippingFee(order.getShippingFee())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus() != null ? order.getStatus().toString() : null)
                .createdAt(order.getCreatedAt())
                .lastModifiedDate(order.getLastModifiedDate())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .build();
    }

   
    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

   
    public OrderItem toEntity(OrderItemResponse response) {
        return OrderItem.builder()
                .id(response.getId())
                .variant(response.getProductId() != null ? com.shopcart.product.entity.ProductVariant.builder().sku(response.getProductId()).build() : null)
                .quantity(response.getQuantity())
                .price(response.getPrice())
                .build();
    }
}
