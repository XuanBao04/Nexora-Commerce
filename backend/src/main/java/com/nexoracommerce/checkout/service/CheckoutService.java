package com.nexoracommerce.checkout.service;

import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.payment.enums.PaymentMethod;
import com.nexoracommerce.payment.enums.TransactionStatus;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.repository.PaymentTransactionRepository;
import com.nexoracommerce.order.service.IOrderService;
import com.nexoracommerce.payment.service.PaymentRollbackService;
import com.nexoracommerce.payment.service.VnPayService;
import com.nexoracommerce.redis.service.RedisStockService;
import com.nexoracommerce.payment.enums.PaymentStatus;
import com.nexoracommerce.mail.service.MailService;
import com.nexoracommerce.inventory.service.IInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


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
public class CheckoutService implements ICheckoutService {

    private final IOrderService orderService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RedisStockService redisStockService;
    private final PaymentRollbackService paymentRollbackService;
    private final OrderRepository orderRepository;
    private final VnPayService vnPayService;
    private final MailService mailService;
    private final IInventoryService inventoryService;
    /**
     * Checkout flow with Redis stock protection:
     * 1. Verify Redis stock available
     * 2. Atomically decrement Redis stock
     * 3. Create order (reserves in DB)
     * 4. Returns order ready for payment
     */
    @Transactional
    @Override
    public OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId, String ipAddress) {
        log.info("Starting checkout with Redis protection: userId={}, itemsCount={}", 
                userId, request.orderItems().size());
        
        try {
            // 1. Check Redis stock for all items before anything else
            checkRedisStockAvailable(request);
            log.debug("Redis stock check passed for user: userId={}", userId);
            
            // 2. Atomically decrement Redis stock for all items
            // If any item fails, exception is thrown and nothing is decremented
            for (var item : request.orderItems()) {
                boolean decremented = redisStockService.decrementIfAvailable(
                        item.variantSku(), 
                        item.quantity()
                );
                
                if (!decremented) {
                    log.warn("Redis stock insufficient after previous check: variantSku={}, quantity={}", 
                            item.variantSku(), item.quantity());
                    throw new RuntimeException("Stock insufficient for product: " + item.variantSku());
                }
            }
            log.debug("Redis stock decremented successfully: userId={}", userId);
            
            // 3. Create order (which reserves stock in DB)
            OrderResponse orderResponse = orderService.createOrder(request, userId);
            log.info("Order created successfully: orderId={}, userId={}", 
                    orderResponse.id(), userId);
            
            // 4. Generate Payment URL if needed
            String paymentUrl = null;
            if (request.paymentMethod() == PaymentMethod.VNPAY) {
                paymentUrl = vnPayService.generatePaymentUrl(orderResponse, ipAddress);
            }
            
            if (paymentUrl != null) {
                return new OrderResponse(
                        orderResponse.id(),
                        orderResponse.userId(),
                        orderResponse.items(),
                        orderResponse.subtotal(),
                        orderResponse.discountAmount(),
                        orderResponse.shippingFee(),
                        orderResponse.totalPrice(),
                        orderResponse.couponCode(),
                        orderResponse.status(),
                        orderResponse.paymentStatus(),
                        orderResponse.customerNote(),
                        orderResponse.createdAt(),
                        orderResponse.lastModifiedDate(),
                        orderResponse.shippingAddress(),
                        orderResponse.phoneNumber(),
                        paymentUrl,
                        orderResponse.paymentTransactions()
                );
            }
            
            return orderResponse;
            
        } catch (Exception e) {
            // Rollback Redis stock if order creation failed
            log.error("Checkout failed, rolling back Redis stock: userId={}, error={}", 
                    userId, e.getMessage());
            
            for (var item : request.orderItems()) {
                try {
                    redisStockService.incrementStock(item.variantSku(), item.quantity());
                } catch (Exception ex) {
                    log.error("Failed to rollback Redis stock: variantSku={}, error={}", 
                            item.variantSku(), ex.getMessage());
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
    @Override
    public PaymentResponse processPayment(String orderId, boolean paymentSuccessful, String providerTransactionId, Long amount, PaymentMethod paymentMethod) {
        log.info("Processing payment for order: orderId={}, success={}", 
                orderId, paymentSuccessful);
        
        Order order = orderRepository.findById(java.util.Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (providerTransactionId != null) {
            var existingTransaction = paymentTransactionRepository.findByProviderTransactionId(providerTransactionId);
            if (existingTransaction.isPresent()) {
                log.info("Payment transaction already processed: orderId={}, providerTransactionId={}",
                        orderId, providerTransactionId);
                return new PaymentResponse(
                        orderId,
                        order.getStatus().name(),
                        "Payment transaction already processed."
                );
            }
        }

        if (paymentSuccessful && order.getPaymentStatus() == PaymentStatus.PAID) {
            log.info("Order is already paid: orderId={}", orderId);
            return new PaymentResponse(
                    orderId,
                    order.getStatus().name(),
                    "Payment already confirmed."
            );
        }

        if (!paymentSuccessful && order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order is already cancelled after failed payment: orderId={}", orderId);
            return new PaymentResponse(
                    orderId,
                    order.getStatus().name(),
                    "Payment failure already processed."
            );
        }
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessLogicException("Order is not in PENDING status: " + orderId);
        }
        
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.VNPAY)
                .amount(amount != null ? amount : order.getTotalPrice())
                .providerTransactionId(providerTransactionId)
                .status(paymentSuccessful ? TransactionStatus.SUCCESS : TransactionStatus.FAILED)
                .build();
        
        paymentTransactionRepository.save(transaction);
        
        if (paymentSuccessful) {
            // Payment succeeded - keep order status as PENDING, but update payment status to PAID
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("Payment confirmed for order: orderId={}", orderId);
            
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            mailService.sendOrderConfirmationEmail(orderId);
                        } catch (Exception e) {
                            log.error("Failed to send order confirmation email asynchronously for orderId: {}", orderId, e);
                        }
                    }
                });
            } else {
                try {
                    mailService.sendOrderConfirmationEmail(orderId);
                } catch (Exception e) {
                    log.error("Failed to trigger order confirmation email for orderId: {}", orderId, e);
                }
            }

            return new PaymentResponse(
                    orderId,
                    OrderStatus.PENDING.name(),
                    "Payment processed successfully. Order is pending admin confirmation."
            );
        } else {
            // Payment failed - rollback stock
            log.warn("Payment failed for order: orderId={}, rolling back stock", orderId);
            paymentRollbackService.rollbackPaymentFailure(order);
            
            // Mark order as cancelled
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);
            log.info("Order cancelled due to payment failure: orderId={}", orderId);
            return new PaymentResponse(
                    orderId,
                    OrderStatus.CANCELLED.name(),
                    "Payment failed. Order cancelled and stock restored."
            );
        }
    }
    
    /**
     * Verifies that all items have sufficient stock in Redis
     * Throws exception if any item is out of stock
     */
    private void checkRedisStockAvailable(OrderRequest request) {
        for (var item : request.orderItems()) {
            long availableStock = redisStockService.getStock(item.variantSku());
            
            if (availableStock < item.quantity()) {
                log.warn("Insufficient Redis stock: variantSku={}, required={}, available={}", 
                        item.variantSku(), item.quantity(), availableStock);
                throw new RuntimeException(
                        "Insufficient stock for product: " + item.variantSku() + 
                        ". Available: " + availableStock + ", Required: " + item.quantity()
                );
            }
        }
    }
}
