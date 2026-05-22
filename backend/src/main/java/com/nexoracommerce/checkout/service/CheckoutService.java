package com.nexoracommerce.checkout.service;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.service.IOrderService;
import com.nexoracommerce.payment.service.PaymentRollbackService;
import com.nexoracommerce.redis.service.RedisStockService;
import com.nexoracommerce.redis.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout service that integrates Redis stock checking with order creation
 * 
 * Flow:
 * 1. Check Redis stock (prevents overselling at cache level)
 * 2. Decrement Redis stock atomically
 * 3. Create order (which reserves stock in DB)
 * 4. Process payment
 * 5. On payment failure → rollback both Redis and DB stock
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final IOrderService orderService;
    private final RedisStockService redisStockService;
    private final StockSyncService stockSyncService;
    private final PaymentRollbackService paymentRollbackService;
    private final OrderRepository orderRepository;
    
    /**
     * Checkout flow with Redis stock protection:
     * 1. Verify Redis stock available
     * 2. Atomically decrement Redis stock
     * 3. Create order (reserves in DB)
     * 4. Returns order ready for payment
     */
    @Transactional
    public OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId) {
        log.info("Starting checkout with Redis protection: userId={}, itemsCount={}", 
                userId, request.getOrderItems().size());
        
        try {
            // 1. Check Redis stock for all items before anything else
            checkRedisStockAvailable(request);
            log.debug("Redis stock check passed for user: userId={}", userId);
            
            // 2. Atomically decrement Redis stock for all items
            // If any item fails, exception is thrown and nothing is decremented
            for (var item : request.getOrderItems()) {
                boolean decremented = redisStockService.decrementIfAvailable(
                        item.getProductId(), 
                        item.getQuantity()
                );
                
                if (!decremented) {
                    log.warn("Redis stock insufficient after previous check: productId={}, quantity={}", 
                            item.getProductId(), item.getQuantity());
                    throw new RuntimeException("Stock insufficient for product: " + item.getProductId());
                }
            }
            log.debug("Redis stock decremented successfully: userId={}", userId);
            
            // 3. Create order (which reserves stock in DB)
            OrderResponse orderResponse = orderService.createOrder(request, userId);
            log.info("Order created successfully: orderId={}, userId={}", 
                    orderResponse.getId(), userId);
            
            return orderResponse;
            
        } catch (Exception e) {
            // Rollback Redis stock if order creation failed
            log.error("Checkout failed, rolling back Redis stock: userId={}, error={}", 
                    userId, e.getMessage());
            
            for (var item : request.getOrderItems()) {
                try {
                    redisStockService.incrementStock(item.getProductId(), item.getQuantity());
                } catch (Exception ex) {
                    log.error("Failed to rollback Redis stock: productId={}, error={}", 
                            item.getProductId(), ex.getMessage());
                }
            }
            
            throw e;
        }
    }
    
    /**
     * Process payment for a pending order
     * On payment success: order transitions to CONFIRMED
     * On payment failure: stock is rolled back from both Redis and DB
     */
    @Transactional
    public void processPayment(String orderId, boolean paymentSuccessful) throws Exception {
        log.info("Processing payment for order: orderId={}, success={}", 
                orderId, paymentSuccessful);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not in PENDING status: " + orderId);
        }
        
        if (paymentSuccessful) {
            // Payment succeeded - transition order to confirmed
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Payment confirmed for order: orderId={}", orderId);
        } else {
            // Payment failed - rollback stock
            log.warn("Payment failed for order: orderId={}, rolling back stock", orderId);
            paymentRollbackService.rollbackPaymentFailure(order);
            
            // Mark order as cancelled
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Order cancelled due to payment failure: orderId={}", orderId);
        }
    }
    
    /**
     * Verifies that all items have sufficient stock in Redis
     * Throws exception if any item is out of stock
     */
    private void checkRedisStockAvailable(OrderRequest request) {
        for (var item : request.getOrderItems()) {
            long availableStock = redisStockService.getStock(item.getProductId());
            
            if (availableStock < item.getQuantity()) {
                log.warn("Insufficient Redis stock: productId={}, required={}, available={}", 
                        item.getProductId(), item.getQuantity(), availableStock);
                throw new RuntimeException(
                        "Insufficient stock for product: " + item.getProductId() + 
                        ". Available: " + availableStock + ", Required: " + item.getQuantity()
                );
            }
        }
    }
}
