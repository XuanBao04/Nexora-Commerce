package com.shopcart.order.service.impl;

import com.shopcart.constant.MessageConstant;
import com.shopcart.order.dto.request.OrderItemRequest;
import com.shopcart.order.dto.request.OrderRequest;
import com.shopcart.order.dto.response.OrderItemResponse;
import com.shopcart.order.dto.response.OrderPreviewResponse;
import com.shopcart.order.dto.response.OrderResponse;
import com.shopcart.order.entity.Order;
import com.shopcart.order.entity.OrderItem;
import com.shopcart.common.enums.OrderStatus;
import com.shopcart.common.exception.ResourceNotFoundException;
import com.shopcart.common.exception.BusinessLogicException;
import com.shopcart.order.mapper.OrderMapper;
import com.shopcart.order.repository.OrderRepository;
import com.shopcart.cart.service.ICartService;
import com.shopcart.order.service.IOrderService;
import com.shopcart.inventory.service.IInventoryService;
import com.shopcart.product.entity.ProductVariant;
import com.shopcart.product.repository.ProductVariantRepository;
import com.shopcart.product.entity.Product;
import com.shopcart.product.repository.ProductRepository;
import com.shopcart.coupon.service.ICouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        long subtotal = calculateSubtotalFromProducts(request.getOrderItems(), productMap);

        // 3. Validate và apply coupon
        long discountAmount = 0L;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.getCouponCode(), subtotal);
            couponCode = request.getCouponCode();
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
                .user(com.shopcart.user.entity.User.builder().id(java.util.UUID.fromString(userId)).build())
                .totalPrice(totalPrice)
                .shippingFee(SHIPPING_FEE)
                .discountAmount(actualDiscount)
                .coupon(couponCode != null ? com.shopcart.coupon.entity.Coupon.builder().code(couponCode).build() : null)
                .shippingAddress(request.getShippingAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .lastModifiedDate(now)
                .orderItems(new ArrayList<>())
                .build();

        // 6. Add order items with DB prices and reserve inventory
        try {
            for (OrderItemRequest itemRequest : request.getOrderItems()) {
                // Reserve stock for order
                inventoryService.reserveStock(itemRequest.getProductId(), itemRequest.getQuantity());

                // Use DB price, not client price
                Product product = productMap.get(itemRequest.getProductId());
                OrderItem orderItem = OrderItem.builder()
                        .variant(com.shopcart.product.entity.ProductVariant.builder().sku(itemRequest.getProductId()).build())
                        .quantity(itemRequest.getQuantity())
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
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(){
        List<Order> orders = orderRepository.findAllWithItems();
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersPageable(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAllWithItemsPageable(pageable);
        return orderPage.map(orderMapper::toOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstant.Order.NOT_FOUND + orderId));
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescWithItems(java.util.UUID.fromString(userId));
        return orders.stream()
                .map(orderMapper::toOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
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
        long subtotal = calculateSubtotalFromProducts(request.getOrderItems(), productMap);

        // Validate and apply coupon
        long discountAmount = 0L;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            discountAmount = couponService.calculateDiscount(request.getCouponCode(), subtotal);
            couponCode = request.getCouponCode();
        }
        
        long actualDiscount = Math.min(subtotal, discountAmount);
        long totalPrice = subtotal - actualDiscount + SHIPPING_FEE;

        // Build preview items with DB prices
        List<OrderItemResponse> previewItems = request.getOrderItems().stream()
                .map(item -> {
                    Product product = productMap.get(item.getProductId());
                    return OrderItemResponse.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .price(product.getPrice())
                            .build();
                })
                .toList();

        return OrderPreviewResponse.builder()
                .userId(request.getUserId())
                .items(previewItems)
                .subtotal(subtotal)
                .discountAmount(actualDiscount)
                .couponCode(couponCode)
                .shippingFee(SHIPPING_FEE)
                .totalPrice(totalPrice)
                .build();
    }

    // ======================== Private Helper Methods ========================

    
    /**
     * Validate order items and return product map with DB prices.
     * SECURITY: Fetch products from DB to verify prices and availability.
     */
    private Map<String, Product> validateOrderItems(OrderRequest request) {
    if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
        throw new BusinessLogicException(MessageConstant.Order.EMPTY_ITEMS);
    }

    // 1. Tổng hợp số lượng yêu cầu theo ID sản phẩm
    Map<String, Integer> requiredQtyByProductId = request.getOrderItems().stream()
            .collect(Collectors.groupingBy(
                    OrderItemRequest::getProductId,
                    Collectors.summingInt(OrderItemRequest::getQuantity)
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
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    /*
     * Calculate subtotal using product prices from database.
     * SECURITY: Fetch DB prices instead of trusting client prices.
     */
    private long calculateSubtotalFromProducts(List<OrderItemRequest> items, Map<String, Product> productMap) {
        return items.stream()
                .mapToLong(item -> {
                    Product product = productMap.get(item.getProductId());
                    return product.getPrice() * item.getQuantity();
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
