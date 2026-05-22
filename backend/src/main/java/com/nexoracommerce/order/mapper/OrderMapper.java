package com.nexoracommerce.order.mapper;

import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderItemResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    default OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = order.getOrderItems() != null
                ? order.getOrderItems().stream()
                    .map(this::toOrderItemResponse)
                    .collect(Collectors.toList())
                : java.util.Collections.emptyList();

        long subtotal = itemResponses.stream()
                .mapToLong(item -> item.price() * item.quantity())
                .sum();

        return new OrderResponse(
                order.getId(),
                order.getUser() != null ? order.getUser().getId().toString() : null,
                itemResponses,
                subtotal,
                order.getDiscountAmount() != null ? order.getDiscountAmount() : 0L,
                order.getShippingFee(),
                order.getTotalPrice(),
                order.getCoupon() != null ? order.getCoupon().getCode() : null,
                order.getStatus() != null ? order.getStatus().toString() : null,
                order.getCreatedAt(),
                order.getLastModifiedDate(),
                order.getShippingAddress(),
                null,
                null,
                null,
                null,
                order.getPhoneNumber()
        );
    }

    default OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getPrice()
        );
    }

    default OrderItem toEntity(OrderItemResponse response) {
        if (response == null) {
            return null;
        }
        return OrderItem.builder()
                .id(response.id())
                .variant(response.productId() != null ? com.nexoracommerce.product.entity.ProductVariant.builder().sku(response.productId()).build() : null)
                .quantity(response.quantity())
                .price(response.price())
                .build();
    }

    List<OrderResponse> toResponseList(List<Order> orders);

    Set<OrderResponse> toResponseSet(Set<Order> orders);
}
