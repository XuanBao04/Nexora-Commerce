package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.cart.service.CartService;
import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.coupon.service.CouponService;
import com.nexoracommerce.inventory.service.InventoryService;
import com.nexoracommerce.order.dto.request.OrderItemRequest;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.request.OrderStatusChangeRequest;
import com.nexoracommerce.order.dto.response.OrderItemResponse;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.service.OrderService;
import com.nexoracommerce.order.service.OrderStatusHistoryService;
import com.nexoracommerce.payment.enums.PaymentMethod;
import com.nexoracommerce.payment.enums.PaymentStatus;
import com.nexoracommerce.payment.enums.TransactionStatus;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.redis.service.RedisStockService;
import com.nexoracommerce.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;
    private final CartService cartService;
    private final CouponService couponService;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final RedisStockService redisStockService;

    private static final long SHIPPING_FEE = 29_900L;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String userId) {
        // Xác thực item + lấy giá từ DB (không tin giá client)
        Map<String, Product> productMap = validateOrderItems(request);
        Map<String, ProductVariant> variantMap = getVariantMap(request);

        long subtotal = calculateSubtotalFromProducts(request.orderItems(), productMap);

        // Áp dụng mã giảm giá
        long discountAmount = 0L;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.couponCode(), subtotal);
            couponCode = request.couponCode();
        }
        long actualDiscount = Math.min(subtotal, discountAmount);
        long totalPrice = subtotal - actualDiscount + SHIPPING_FEE;

        // Xóa giỏ hàng trước để tránh giữ tồn kho trùng lặp
        cartService.clearCart(userId);

        String orderId = generateOrderId();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        Order order = Order.builder()
                .id(orderId)
                .user(User.builder().id(UUID.fromString(userId)).build())
                .totalPrice(totalPrice)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(actualDiscount)
                .coupon(couponCode != null
                        ? com.nexoracommerce.coupon.entity.Coupon.builder().code(couponCode).build()
                        : null)
                .shippingAddress(request.shippingAddress())
                .phoneNumber(request.phoneNumber())
                .customerNote(request.customerNote())
                .paymentStatus(PaymentStatus.UNPAID)
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .lastModifiedDate(now)
                .orderItems(new HashSet<>())
                .statusHistory(new HashSet<>())
                .paymentTransactions(new HashSet<>())
                .build();

        // Giữ tồn kho + tạo order item
        try {
            for (OrderItemRequest itemRequest : request.orderItems()) {
                inventoryService.reserveStock(itemRequest.variantSku(), itemRequest.quantity());

                Product product = productMap.get(itemRequest.variantSku());
                ProductVariant variant = variantMap.get(itemRequest.variantSku());

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .variant(variant)
                        .productName(itemRequest.productName())
                        .variantName(itemRequest.variantName())
                        .quantity(itemRequest.quantity())
                        .price(itemRequest.price() != null ? itemRequest.price() : product.getPrice())
                        .build();

                order.getOrderItems().add(orderItem);
            }

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .order(order)
                    .paymentMethod(request.paymentMethod())
                    .amount(totalPrice)
                    .status(TransactionStatus.PENDING)
                    .createdAt(now)
                    .build();
            order.getPaymentTransactions().add(transaction);

            Order savedOrder = orderRepository.save(order);

            orderStatusHistoryService.recordStatusChange(
                orderId, OrderStatus.PENDING.toString(), "SYSTEM",
                "Order created with payment method: " + request.paymentMethod()
            );

            return orderMapper.toOrderResponse(savedOrder);
        } catch (Exception e) {
            // Rollback: giải phóng tồn kho đã giữ nếu tạo đơn thất bại
            for (OrderItem item : order.getOrderItems()) {
                try {
                    inventoryService.releaseStock(item.getVariant().getSku(), item.getQuantity());
                } catch (Exception ex) {
                    log.error("Failed to rollback stock for SKU: {}", item.getVariant().getSku(), ex);
                }
            }
            throw e;
        }
    }

    @Override
    public OrderResponse getOrderById(String orderId, String userId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));
        if (!order.getUser().getId().toString().equals(userId)) {
            throw new BusinessLogicException("Bạn không có quyền xem đơn hàng này");
        }
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public OrderResponse getOrderByIdForAdmin(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public Page<OrderResponse> getOrdersWithFilters(String userId, OrderStatus status, PaymentStatus paymentStatus, String couponCode, LocalDateTime startDate, LocalDateTime endDate, Long minPrice, Pageable pageable) {
        UUID uid = userId != null ? UUID.fromString(userId) : null;
        return orderRepository.findOrdersWithFilters(uid, status, paymentStatus, couponCode, startDate, endDate, minPrice, pageable)
                .map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, String userId, String reason) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));

        if (!order.getUser().getId().toString().equals(userId)) {
            throw new BusinessLogicException("Bạn không có quyền hủy đơn hàng này");
        }

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new BusinessLogicException(MessageConstant.Order.CANNOT_CANCEL + order.getStatus());
        }

        // Giải phóng tồn kho DB + Redis
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getVariant().getSku(), item.getQuantity());
            try {
                redisStockService.incrementStock(item.getVariant().getSku(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore Redis stock: orderId={}, sku={}", orderId, item.getVariant().getSku(), e);
            }
        }

        // Hoàn tiền nếu đã thanh toán
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        orderStatusHistoryService.recordStatusChange(
            orderId, OrderStatus.CANCELLED.toString(), "SYSTEM", reason != null ? reason : "Order cancelled by user"
        );

        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatusChangeRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + orderId));

        OrderStatus newStatus;
        try {
            newStatus = request.newStatus();
        } catch (Exception e) {
            throw new BusinessLogicException(MessageConstant.Order.INVALID_STATUS + request.newStatus());
        }

        validateStatusTransition(order.getStatus(), newStatus);
        updateInventoryByStatusTransition(order, newStatus);

        order.setStatus(newStatus);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);

        orderStatusHistoryService.recordStatusChange(
            orderId, newStatus.toString(), "ADMIN", request.reason()
        );

        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(String orderId) {
        return orderStatusHistoryService.getOrderStatusHistory(orderId);
    }

    @Override
    public OrderPreviewResponse previewOrder(OrderRequest request) {
        Map<String, Product> productMap = validateOrderItems(request);
        long subtotal = calculateSubtotalFromProducts(request.orderItems(), productMap);

        long discountAmount = 0L;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.couponCode(), subtotal);
            couponCode = request.couponCode();
        }

        long actualDiscount = Math.min(subtotal, discountAmount);
        long totalPrice = subtotal - actualDiscount + SHIPPING_FEE;

        List<OrderItemResponse> previewItems = request.orderItems().stream()
                .map(item -> {
                    Product product = productMap.get(item.variantSku());
                    return new OrderItemResponse(null, item.variantSku(), item.productName(),
                            item.variantName(), item.quantity(), product.getPrice());
                })
                .toList();

        return new OrderPreviewResponse(request.userId(), previewItems, subtotal,
                actualDiscount, SHIPPING_FEE, totalPrice, couponCode);
    }

    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Xác thực item đơn hàng: kiểm tra tồn tại + đủ tồn kho
    private Map<String, Product> validateOrderItems(OrderRequest request) {
        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new BusinessLogicException(MessageConstant.Order.EMPTY_ITEMS);
        }

        Map<String, Integer> requiredQtyByVariantSku = request.orderItems().stream()
                .collect(Collectors.groupingBy(OrderItemRequest::variantSku,
                        Collectors.summingInt(OrderItemRequest::quantity)));

        Set<String> variantSkus = requiredQtyByVariantSku.keySet();

        List<ProductVariant> variants = productVariantRepository.findBySkuIn(variantSkus);
        if (variants.size() != variantSkus.size()) {
            Set<String> existingSkus = variants.stream()
                    .map(ProductVariant::getSku).collect(Collectors.toSet());
            String missingSku = variantSkus.stream()
                    .filter(sku -> !existingSkus.contains(sku))
                    .findFirst().orElse("Unknown");
            throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + missingSku);
        }

        for (ProductVariant variant : variants) {
            int requiredQty = requiredQtyByVariantSku.get(variant.getSku());
            int reserved = variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
            int available = variant.getQuantity() - reserved;

            if (available < requiredQty) {
                throw new BusinessLogicException(
                    MessageConstant.Product.INSUFFICIENT_STOCK + variant.getSku());
            }
        }

        return variants.stream()
                .map(ProductVariant::getProduct)
                .collect(Collectors.toMap(Product::getId, product -> product, (p1, p2) -> p1));
    }

    private Map<String, ProductVariant> getVariantMap(OrderRequest request) {
        Set<String> variantSkus = request.orderItems().stream()
                .map(OrderItemRequest::variantSku).collect(Collectors.toSet());
        return productVariantRepository.findBySkuIn(variantSkus).stream()
                .collect(Collectors.toMap(ProductVariant::getSku, variant -> variant));
    }

    // Tính tổng tiền từ giá DB (không tin giá client)
    private long calculateSubtotalFromProducts(List<OrderItemRequest> items, Map<String, Product> productMap) {
        return items.stream()
                .mapToLong(item -> productMap.get(item.variantSku()).getPrice() * item.quantity())
                .sum();
    }

    // Kiểm tra chuyển trạng thái hợp lệ
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new BusinessLogicException("Order is already in status: " + currentStatus);
        }

        boolean allowed = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new BusinessLogicException("Invalid status transition: " + currentStatus + " → " + newStatus);
        }
    }

    // Cập nhật tồn kho theo chuyển trạng thái đơn hàng
    private void updateInventoryByStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        // PENDING → CONFIRMED: xác nhận tồn kho
        if (newStatus == OrderStatus.CONFIRMED && currentStatus == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.confirmStock(item.getVariant().getSku(), item.getQuantity());
            }
        }

        // PROCESSING → SHIPPED: trừ kho
        if (newStatus == OrderStatus.SHIPPED && currentStatus == OrderStatus.PROCESSING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.shipStock(item.getVariant().getSku(), item.getQuantity());
            }
        }

        // Hủy đơn: hoàn tồn kho DB + Redis
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.releaseStock(item.getVariant().getSku(), item.getQuantity());
                try {
                    redisStockService.incrementStock(item.getVariant().getSku(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Admin cancel - failed to restore Redis stock: orderId={}, sku={}",
                            order.getId(), item.getVariant().getSku(), e);
                }
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }
    }
}
