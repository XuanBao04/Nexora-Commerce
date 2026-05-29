package com.nexoracommerce.order.mapper;

import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderItemResponse;
import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import com.nexoracommerce.order.dto.response.PaymentTransactionResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.order.entity.OrderStatusHistory;
import com.nexoracommerce.order.entity.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    /**
     * Convert Order entity to OrderResponse DTO
     */
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
                order.getStatus(),                          // OrderStatus enum
                order.getPaymentStatus(),                   // PaymentStatus enum
                order.getCustomerNote(),                    // NEW: Customer note
                order.getCreatedAt(),
                order.getLastModifiedDate(),
                order.getShippingAddress(),
                order.getPhoneNumber()
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponse DTO
     */
    default OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getVariant() != null ? item.getVariant().getSku() : null,  // variantSku
                item.getProductName(),                      // Snapshot at checkout
                item.getVariantName(),                      // Snapshot of variant info
                item.getQuantity(),
                item.getPrice()
        );
    }

    /**
     * Convert OrderStatusHistory entity to OrderStatusHistoryResponse DTO
     */
    default OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getOrder() != null ? history.getOrder().getId() : null,
                history.getStatus(),
                history.getChangedBy(),
                history.getReason(),
                history.getCreatedAt()
        );
    }

    /**
     * Convert PaymentTransaction entity to PaymentTransactionResponse DTO
     */
    default PaymentTransactionResponse toPaymentTransactionResponse(PaymentTransaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getOrder() != null ? transaction.getOrder().getId() : null,
                transaction.getPaymentMethod(),
                transaction.getAmount(),
                transaction.getProviderTransactionId(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponse DTO (overload)
     */
    default OrderItem toEntity(OrderItemResponse response) {
        if (response == null) {
            return null;
        }
        return OrderItem.builder()
                .id(response.id())
                .variant(response.variantSku() != null ? 
                    com.nexoracommerce.product.entity.ProductVariant.builder()
                        .sku(response.variantSku())
                        .build() 
                    : null)
                .productName(response.productName())
                .variantName(response.variantName())
                .quantity(response.quantity())
                .price(response.price())
                .build();
    }

    List<OrderResponse> toResponseList(List<Order> orders);

    Set<OrderResponse> toResponseSet(Set<Order> orders);

    /**
     * Convert OrderStatusHistory list to OrderStatusHistoryResponse list
     */
    default List<OrderStatusHistoryResponse> toStatusHistoryResponseList(List<OrderStatusHistory> histories) {
        if (histories == null) {
            return java.util.Collections.emptyList();
        }
        return histories.stream()
                .map(this::toOrderStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert PaymentTransaction list to PaymentTransactionResponse list
     */
    default List<PaymentTransactionResponse> toPaymentTransactionResponseList(List<PaymentTransaction> transactions) {
        if (transactions == null) {
            return java.util.Collections.emptyList();
        }
        return transactions.stream()
                .map(this::toPaymentTransactionResponse)
                .collect(Collectors.toList());
    }
}
