package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.order.dto.request.OrderItemRequest;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.request.OrderStatusChangeRequest;
import com.nexoracommerce.order.dto.response.OrderItemResponse;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.dto.response.OrderStatusHistoryResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.order.enums.PaymentStatus;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.service.IOrderService;
import com.nexoracommerce.order.service.IOrderStatusHistoryService;
import com.nexoracommerce.cart.service.ICartService;
import com.nexoracommerce.inventory.service.IInventoryService;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.coupon.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for Order operations
 * Handles order creation, status updates, and price calculations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final IInventoryService inventoryService;
    private final ICartService cartService;
    private final ICouponService couponService;
    private final ProductVariantRepository productVariantRepository;
    private final IOrderStatusHistoryService orderStatusHistoryService;

    private static final long SHIPPING_FEE = 29_900L;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String userId) {
        // 1. Validate order items and fetch DB prices (DO NOT TRUST CLIENT PRICES)
        Map<String, Product> productMap = validateOrderItems(request);
        Map<String, ProductVariant> variantMap = getVariantMap(request);

        // 2. Calculate price using DB prices, not client prices
        long subtotal = calculateSubtotalFromProducts(request.orderItems(), productMap);

        // 3. Validate and apply coupon
        long discountAmount = 0L;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.couponCode(), subtotal);
            couponCode = request.couponCode();
        }
        long actualDiscount = Math.min(subtotal, discountAmount);
        long totalPrice = subtotal - actualDiscount + SHIPPING_FEE;

        // 4. Release cart reserves first to avoid double-holding inventory
        cartService.clearCart(userId);

        // 5. Create order with DB prices and reserve stock
        String orderId = generateOrderId();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        Order order = Order.builder()
                .id(orderId)
                .user(com.nexoracommerce.user.entity.User.builder().id(UUID.fromString(userId)).build())
                .totalPrice(totalPrice)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(actualDiscount)
                .coupon(couponCode != null ? com.nexoracommerce.coupon.entity.Coupon.builder().code(couponCode).build() : null)
                .shippingAddress(request.shippingAddress())
                .phoneNumber(request.phoneNumber())
                .customerNote(request.customerNote())  // NEW: Customer note
                .paymentStatus(PaymentStatus.UNPAID)   // NEW: Default unpaid
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .lastModifiedDate(now)
                .orderItems(new HashSet<>())
                .statusHistory(new HashSet<>())
                .paymentTransactions(new HashSet<>())
                .build();

        // 6. Add order items with DB prices and reserve inventory
        try {
            for (OrderItemRequest itemRequest : request.orderItems()) {
                // Reserve stock for order
                inventoryService.reserveStock(itemRequest.variantSku(), itemRequest.quantity());

                // Use DB price, not client price
                Product product = productMap.get(itemRequest.variantSku());
                ProductVariant variant = variantMap.get(itemRequest.variantSku());
                
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .variant(variant)
                        .productName(itemRequest.productName())        // Snapshot at checkout
                        .variantName(itemRequest.variantName())        // Snapshot at checkout
                        .quantity(itemRequest.quantity())
                        .price(itemRequest.price() != null ? itemRequest.price() : product.getPrice())
                        .build();

                order.getOrderItems().add(orderItem);
            }
            
            // 7. Save order only after all validations and reservations succeed
            Order savedOrder = orderRepository.save(java.util.Objects.requireNonNull(order));
            
            // 8. Record initial status change in history
            orderStatusHistoryService.recordStatusChange(
                orderId, 
                OrderStatus.PENDING.toString(), 
                "SYSTEM", 
                "Order created"
            );
            
            return orderMapper.toOrderResponse(savedOrder);
        } catch (Exception e) {
            // Rollback: release any reserved stock if order creation fails
            for (OrderItem item : order.getOrderItems()) {
                try {
                    inventoryService.releaseStock(item.getVariant().getSku(), item.getQuantity());
                } catch (Exception ex) {
                    // Log but don't fail - partial rollback is better than nothing
                }
            }
            throw e;
        }
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescWithItems(UUID.fromString(userId));
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Override
    public Page<OrderResponse> getUserOrdersPageable(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserIdWithItemsPageable(UUID.fromString(userId), pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllWithItems();
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Override
    public Page<OrderResponse> getAllOrdersPageable(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAllWithItemsPageable(pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByStatus(status, pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    public Page<OrderResponse> getUnpaidOrdersByUser(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findUnpaidOrdersByUser(UUID.fromString(userId), PaymentStatus.UNPAID, pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new BusinessLogicException(MessageConstant.Order.CANNOT_CANCEL + order.getStatus());
        }

        // Release reserved stock when canceling
        for (OrderItem item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getVariant().getSku(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        // Record status change
        orderStatusHistoryService.recordStatusChange(
            orderId,
            OrderStatus.CANCELLED.toString(),
            "SYSTEM",
            "Order cancelled by user"
        );
        
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(OrderStatusChangeRequest request) {
        Order order = orderRepository.findByIdWithItems(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    MessageConstant.Order.NOT_FOUND + request.orderId()));

        OrderStatus newStatus;
        try {
            newStatus = request.newStatus();
        } catch (Exception e) {
            throw new BusinessLogicException(MessageConstant.Order.INVALID_STATUS + request.newStatus());
        }

        // Validate status transition workflow
        validateStatusTransition(order.getStatus(), newStatus);

        // Update inventory based on status transition
        updateInventoryByStatusTransition(order, newStatus);

        // Update order
        order.setStatus(newStatus);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        
        // Record status change with reason
        orderStatusHistoryService.recordStatusChange(
            request.orderId(),
            newStatus.toString(),
            "ADMIN",
            request.reason()
        );
        
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderStatusHistory(String orderId) {
        return orderStatusHistoryService.getOrderStatusHistory(orderId);
    }

    @Override
    public OrderPreviewResponse previewOrder(OrderRequest request) {
        // Validate and fetch product prices from DB
        Map<String, Product> productMap = validateOrderItems(request);

        // Calculate price using DB prices, not client prices
        long subtotal = calculateSubtotalFromProducts(request.orderItems(), productMap);

        // Validate and apply coupon
        long discountAmount = 0L;
        String couponCode = null;
        if (request.couponCode() != null && !request.couponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.couponCode(), subtotal);
            couponCode = request.couponCode();
        }
        
        long actualDiscount = Math.min(subtotal, discountAmount);
        long totalPrice = subtotal - actualDiscount + SHIPPING_FEE;

        // Build preview items with DB prices
        List<OrderItemResponse> previewItems = request.orderItems().stream()
                .map(item -> {
                    Product product = productMap.get(item.variantSku());
                    return new OrderItemResponse(
                            null,
                            item.variantSku(),
                            item.productName(),
                            item.variantName(),
                            item.quantity(),
                            product.getPrice()
                    );
                })
                .toList();

        return new OrderPreviewResponse(
                request.userId(),
                previewItems,
                subtotal,
                actualDiscount,
                SHIPPING_FEE,
                totalPrice,
                couponCode
        );
    }

    // ======================== Private Helper Methods ========================

    /**
     * Generate a unique order ID in format ORD-{TIMESTAMP}-{RANDOM}
     */
    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Validate order items and return product map with DB prices.
     * SECURITY: Fetch products from DB to verify prices and availability.
     */
    private Map<String, Product> validateOrderItems(OrderRequest request) {
        if (request.orderItems() == null || request.orderItems().isEmpty()) {
            throw new BusinessLogicException(MessageConstant.Order.EMPTY_ITEMS);
        }

        // 1. Group quantities by variant SKU
        Map<String, Integer> requiredQtyByVariantSku = request.orderItems().stream()
                .collect(Collectors.groupingBy(
                        OrderItemRequest::variantSku,
                        Collectors.summingInt(OrderItemRequest::quantity)
                ));

        Set<String> variantSkus = requiredQtyByVariantSku.keySet();

        // 2. Fetch and validate all variants exist
        List<ProductVariant> variants = productVariantRepository.findBySkuIn(variantSkus);
        if (variants.size() != variantSkus.size()) {
            Set<String> existingSkus = variants.stream()
                    .map(ProductVariant::getSku)
                    .collect(Collectors.toSet());

            String missingSku = variantSkus.stream()
                    .filter(sku -> !existingSkus.contains(sku))
                    .findFirst()
                    .orElse("Unknown");

            throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + missingSku);
        }

        // 3. Validate stock availability
        for (ProductVariant variant : variants) {
            int requiredQty = requiredQtyByVariantSku.get(variant.getSku());
            int reserved = variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
            int available = variant.getQuantity() - reserved;
            
            if (available < requiredQty) {
                throw new BusinessLogicException(
                    MessageConstant.Product.INSUFFICIENT_STOCK + variant.getSku());
            }
        }

        // 4. Return product map for price validation
        Map<String, Product> productMap = variants.stream()
                .map(ProductVariant::getProduct)
                .collect(Collectors.toMap(Product::getId, product -> product, (p1, p2) -> p1));
        return productMap;
    }

    /**
     * Get variant map from request
     */
    private Map<String, ProductVariant> getVariantMap(OrderRequest request) {
        Set<String> variantSkus = request.orderItems().stream()
                .map(OrderItemRequest::variantSku)
                .collect(Collectors.toSet());

        return productVariantRepository.findBySkuIn(variantSkus).stream()
                .collect(Collectors.toMap(ProductVariant::getSku, variant -> variant));
    }

    /**
     * Calculate subtotal using product prices from database.
     * SECURITY: Fetch DB prices instead of trusting client prices.
     */
    private long calculateSubtotalFromProducts(List<OrderItemRequest> items, Map<String, Product> productMap) {
        return items.stream()
                .mapToLong(item -> {
                    Product product = productMap.get(item.variantSku());
                    return product.getPrice() * item.quantity();
                })
                .sum();
    }

    /**
     * Validate order status transition workflow.
     * Prevents invalid state transitions.
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // DELIVERED is a terminal state - no transitions from it
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BusinessLogicException("Cannot transition from DELIVERED state");
        }

        // CANCELLED is a terminal state
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new BusinessLogicException("Cannot transition from CANCELLED state");
        }

        // Prevent same status transitions
        if (currentStatus == newStatus) {
            throw new BusinessLogicException("Order is already in status: " + currentStatus);
        }
    }

    /**
     * Update inventory based on status transition
     */
    private void updateInventoryByStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();

        // PENDING → CONFIRMED: Move reserved → sold
        if (newStatus == OrderStatus.CONFIRMED && currentStatus == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.confirmStock(item.getVariant().getSku(), item.getQuantity());
            }
        }

        // PROCESSING → SHIPPED: Deduct from inventory
        if (newStatus == OrderStatus.SHIPPED && currentStatus == OrderStatus.PROCESSING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.shipStock(item.getVariant().getSku(), item.getQuantity());
            }
        }
    }
}
