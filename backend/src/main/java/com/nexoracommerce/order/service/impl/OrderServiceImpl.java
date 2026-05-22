package com.nexoracommerce.order.service.impl;

import com.nexoracommerce.constant.MessageConstant;
import com.nexoracommerce.order.dto.request.OrderItemRequest;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderItemResponse;
import com.nexoracommerce.order.dto.response.OrderPreviewResponse;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.OrderItem;
import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.order.mapper.OrderMapper;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.cart.service.ICartService;
import com.nexoracommerce.order.service.IOrderService;
import com.nexoracommerce.inventory.service.IInventoryService;
import com.nexoracommerce.product.entity.ProductVariant;
import com.nexoracommerce.product.repository.ProductVariantRepository;
import com.nexoracommerce.product.entity.Product;
import com.nexoracommerce.product.repository.ProductRepository;
import com.nexoracommerce.coupon.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for Order operations
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
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    private static final long SHIPPING_FEE = 29_900L;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, String userId) {
        // 1. Validate order items and fetch DB prices (DO NOT TRUST CLIENT PRICES)
        Map<String, Product> productMap = validateOrderItems(request);

        // 2. Calculate price using DB prices, not client prices
        long subtotal = calculateSubtotalFromProducts(request.orderItems(), productMap);

        // 3. Validate và apply coupon
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
        String orderId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        Order order = Order.builder()
                .id(orderId)
                .user(com.nexoracommerce.user.entity.User.builder().id(java.util.UUID.fromString(userId)).build())
                .totalPrice(totalPrice)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(actualDiscount)
                .coupon(couponCode != null ? com.nexoracommerce.coupon.entity.Coupon.builder().code(couponCode).build() : null)
                .shippingAddress(request.shippingAddress())
                .phoneNumber(request.phoneNumber())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .lastModifiedDate(now)
                .orderItems(new ArrayList<>())
                .build();

        // 6. Add order items with DB prices and reserve inventory
        try {
            for (OrderItemRequest itemRequest : request.orderItems()) {
                // Reserve stock for order
                inventoryService.reserveStock(itemRequest.productId(), itemRequest.quantity());

                // Use DB price, not client price
                Product product = productMap.get(itemRequest.productId());
                OrderItem orderItem = OrderItem.builder()
                        .variant(com.nexoracommerce.product.entity.ProductVariant.builder().sku(itemRequest.productId()).build())
                        .quantity(itemRequest.quantity())
                        .price(product.getPrice())
                        .order(order)
                        .build();

                order.getOrderItems().add(orderItem);
            }
            // 7. Save order only after all validations and reservations succeed
            Order savedOrder = orderRepository.save(order);
            return orderMapper.toOrderResponse(savedOrder);
        } catch (Exception e) {
            // Rollback: release any reserved stock if order creation fails
            for (OrderItem item : order.getOrderItems()) {
                try {
                    inventoryService.releaseStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    // Log but don't fail - partial rollback is better than nothing
                }
            }
            throw e;
        }
    }
    @Override
    public List<OrderResponse> getAllOrders(){
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
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescWithItems(java.util.UUID.fromString(userId));
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Override
    public Page<OrderResponse> getUserOrdersPageable(String userId, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findByUserIdWithItemsPageable(java.util.UUID.fromString(userId), pageable);
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

        // Hủy đơn / timeout -> giảm reserved_stock
        if (order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.releaseStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessLogicException(MessageConstant.Order.INVALID_STATUS + status);
        }

        // Validate status transition workflow
        validateStatusTransition(order.getStatus(), newStatus);

        // PENDING → CONFIRMED: Move reserved → sold
        if (newStatus == OrderStatus.CONFIRMED && order.getStatus() == OrderStatus.PENDING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.confirmStock(item.getProductId(), item.getQuantity());
            }
        }

        // PROCESSING → SHIPPED: Deduct from inventory
        if (newStatus == OrderStatus.SHIPPED && order.getStatus() == OrderStatus.PROCESSING) {
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.shipStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(newStatus);
        order.setLastModifiedDate(LocalDateTime.now());
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
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
                    Product product = productMap.get(item.productId());
                    return new OrderItemResponse(
                            null,
                            item.productId(),
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
     * Validate order items and return product map with DB prices.
     * SECURITY: Fetch products from DB to verify prices and availability.
     */
    private Map<String, Product> validateOrderItems(OrderRequest request) {
    if (request.orderItems() == null || request.orderItems().isEmpty()) {
        throw new BusinessLogicException(MessageConstant.Order.EMPTY_ITEMS);
    }

    // 1. Tổng hợp số lượng yêu cầu theo ID sản phẩm
    Map<String, Integer> requiredQtyByProductId = request.orderItems().stream()
            .collect(Collectors.groupingBy(
                    OrderItemRequest::productId,
                    Collectors.summingInt(OrderItemRequest::quantity)
            ));

    Set<String> productIds = requiredQtyByProductId.keySet();

    // 2. Xác thực rằng tất cả các sản phẩm được yêu cầu đều tồn tại trong database
    List<Product> products = productRepository.findAllById(productIds);
    if (products.size() != productIds.size()) {
        Set<String> existingProductIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        // Tìm ID bị thiếu đầu tiên để trả về thông báo lỗi hữu ích
        String missingProductId = productIds.stream()
                .filter(id -> !existingProductIds.contains(id))
                .findFirst()
                .orElse("Unknown");

        throw new ResourceNotFoundException(MessageConstant.Product.NOT_FOUND + missingProductId);
    }

    // 3. Lấy thông tin tồn kho và ánh xạ chúng theo ID sản phẩm
    Map<String, ProductVariant> variantBySku = productVariantRepository.findBySkuIn(productIds).stream()
            .collect(Collectors.toMap(ProductVariant::getSku, variant -> variant));

    // 4. Xác thực xem có đủ hàng tồn kho cho từng sản phẩm được yêu cầu hay không
    requiredQtyByProductId.forEach((productId, requiredQuantity) -> {
        ProductVariant variant = variantBySku.get(productId);
        
        if (variant == null) {
            throw new BusinessLogicException(MessageConstant.Product.INSUFFICIENT_STOCK + productId);
        }

        int reserved = variant.getReservedQuantity() == null ? 0 : variant.getReservedQuantity();
        int available = variant.getQuantity() - reserved;
        
        if (available < requiredQuantity) {
            throw new BusinessLogicException(MessageConstant.Product.INSUFFICIENT_STOCK + productId);
        }
    });

        // 5. Return product map for price validation
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        return productMap;
    }

    /*
     * Tính tổng tiền hàng (chưa bao gồm phí ship).
     */
    private long calculateSubtotal(List<OrderItemRequest> items) {
        return items.stream()
                .mapToLong(item -> item.price() * item.quantity())
                .sum();
    }

    /*
     * Calculate subtotal using product prices from database.
     * SECURITY: Fetch DB prices instead of trusting client prices.
     */
    private long calculateSubtotalFromProducts(List<OrderItemRequest> items, Map<String, Product> productMap) {
        return items.stream()
                .mapToLong(item -> {
                    Product product = productMap.get(item.productId());
                    return product.getPrice() * item.quantity();
                })
                .sum();
    }

    /*
     * Validate order status transition workflow.
     * Prevents invalid state transitions.
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // DELIVERED is a terminal state - no transitions from it
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new BusinessLogicException("Cannot transition from DELIVERED state");
        }

        // Prevent same status transitions
        if (currentStatus == newStatus) {
            throw new BusinessLogicException("Order is already in status: " + currentStatus);
        }
    }
}
