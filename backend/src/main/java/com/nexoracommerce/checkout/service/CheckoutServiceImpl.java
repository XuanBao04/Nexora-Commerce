package com.nexoracommerce.checkout.service;

import com.nexoracommerce.checkout.dto.PaymentResponse;
import com.nexoracommerce.common.enums.OrderStatus;
import com.nexoracommerce.common.exception.BusinessLogicException;
import com.nexoracommerce.common.exception.ResourceNotFoundException;
import com.nexoracommerce.inventory.service.InventoryService;
import com.nexoracommerce.mail.service.MailService;
import com.nexoracommerce.order.dto.request.OrderRequest;
import com.nexoracommerce.order.dto.response.OrderResponse;
import com.nexoracommerce.order.entity.Order;
import com.nexoracommerce.order.entity.PaymentTransaction;
import com.nexoracommerce.order.repository.OrderRepository;
import com.nexoracommerce.order.repository.PaymentTransactionRepository;
import com.nexoracommerce.order.service.OrderService;
import com.nexoracommerce.payment.enums.PaymentMethod;
import com.nexoracommerce.payment.enums.PaymentStatus;
import com.nexoracommerce.payment.enums.TransactionStatus;
import com.nexoracommerce.payment.service.PaymentRollbackService;
import com.nexoracommerce.payment.service.VnPayService;
import com.nexoracommerce.redis.service.RedisStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.nexoracommerce.order.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final OrderService orderService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RedisStockService redisStockService;
    private final PaymentRollbackService paymentRollbackService;
    private final OrderRepository orderRepository;
    private final VnPayService vnPayService;
    private final MailService mailService;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Transactional
    @Override
    public OrderResponse checkoutWithRedisProtection(OrderRequest request, String userId, String ipAddress) {
        log.info("Starting checkout: userId={}, itemsCount={}", userId, request.orderItems().size());

        try {
            // Kiểm tra + trừ tồn kho Redis
            checkRedisStockAvailable(request);

            for (var item : request.orderItems()) {
                boolean decremented = redisStockService.decrementIfAvailable(item.variantSku(), item.quantity());
                if (!decremented) {
                    throw new RuntimeException("Stock insufficient for product: " + item.variantSku());
                }
            }

            // Tạo đơn hàng (giữ tồn kho trong DB)
            OrderResponse orderResponse = orderService.createOrder(request, userId);
            log.info("Order created: orderId={}", orderResponse.id());

            // Tạo URL thanh toán nếu VnPay
            String paymentUrl = null;
            if (request.paymentMethod() == PaymentMethod.VNPAY) {
                paymentUrl = vnPayService.generatePaymentUrl(orderResponse, ipAddress);
            }

            if (paymentUrl != null) {
                return new OrderResponse(
                        orderResponse.id(), orderResponse.userId(), orderResponse.items(),
                        orderResponse.subtotal(), orderResponse.discountAmount(),
                        orderResponse.shippingFee(), orderResponse.totalPrice(),
                        orderResponse.couponCode(), orderResponse.status(),
                        orderResponse.paymentStatus(), orderResponse.customerNote(),
                        orderResponse.createdAt(), orderResponse.lastModifiedDate(),
                        orderResponse.shippingAddress(), orderResponse.phoneNumber(),
                        paymentUrl, orderResponse.paymentTransactions()
                );
            }

            return orderResponse;

        } catch (Exception e) {
            // Hoàn trả tồn kho Redis nếu thất bại
            log.error("Checkout failed, rolling back Redis stock: userId={}", userId, e);
            for (var item : request.orderItems()) {
                try {
                    redisStockService.incrementStock(item.variantSku(), item.quantity());
                } catch (Exception ex) {
                    log.error("Failed to rollback Redis stock: sku={}", item.variantSku(), ex);
                }
            }
            throw e;
        }
    }

    @Transactional
    @Override
    public PaymentResponse processPayment(String orderId, boolean paymentSuccessful,
                                           String providerTransactionId, Long amount,
                                           PaymentMethod paymentMethod) {
        log.info("Processing payment: orderId={}, success={}", orderId, paymentSuccessful);

        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        // Kiểm tra giao dịch đã xử lý (idempotent)
        if (providerTransactionId != null) {
            var existingTransaction = paymentTransactionRepository.findByProviderTransactionId(providerTransactionId);
            if (existingTransaction.isPresent()) {
                return new PaymentResponse(orderId, order.getStatus().name(), "Payment transaction already processed.");
            }
        }

        if (paymentSuccessful && order.getPaymentStatus() == PaymentStatus.PAID) {
            return new PaymentResponse(orderId, order.getStatus().name(), "Payment already confirmed.");
        }

        if (!paymentSuccessful && order.getStatus() == OrderStatus.CANCELLED) {
            return new PaymentResponse(orderId, order.getStatus().name(), "Payment failure already processed.");
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
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            // Gửi email xác nhận sau khi commit transaction
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            mailService.sendOrderConfirmationEmail(orderId);
                        } catch (Exception e) {
                            log.error("Failed to send confirmation email: orderId={}", orderId, e);
                        }
                    }
                });
            } else {
                try {
                    mailService.sendOrderConfirmationEmail(orderId);
                } catch (Exception e) {
                    log.error("Failed to send confirmation email: orderId={}", orderId, e);
                }
            }

            return new PaymentResponse(orderId, OrderStatus.PENDING.name(),
                    "Payment processed successfully. Order is pending admin confirmation.");
        } else {
            // Thanh toán trực tuyến thất bại → giữ nguyên trạng thái PENDING + UNPAID để khách hàng có thể thanh toán lại.
            // Scheduler (PaymentTimeoutScheduler) sẽ tự động quét và hủy đơn/hoàn kho sau 15 phút nếu vẫn chưa thanh toán.
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);

            return new PaymentResponse(orderId, OrderStatus.PENDING.name(),
                    "Payment failed. Order is kept active for repayment attempt.");
        }
    }
    @Transactional
    @Override
    public OrderResponse repayPayment(String orderId, String ipAddress) {
        log.info("Initiating repayment for orderId={}", orderId);

        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessLogicException("Only pending orders can be repaid.");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessLogicException("Order has already been paid.");
        }

        // Tạo transaction mới cho lần thanh toán lại
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        PaymentTransaction transaction = PaymentTransaction.builder()
                .order(order)
                .paymentMethod(PaymentMethod.VNPAY)
                .amount(order.getTotalPrice())
                .status(TransactionStatus.PENDING)
                .createdAt(now)
                .build();
        paymentTransactionRepository.save(transaction);
        order.getPaymentTransactions().add(transaction);

        // Convert sang DTO thông qua OrderMapper
        OrderResponse orderResponse = orderMapper.toOrderResponse(order);

        // Sinh link thanh toán VNPay mới
        String paymentUrl = vnPayService.generatePaymentUrl(orderResponse, ipAddress);

        return new OrderResponse(
                orderResponse.id(), orderResponse.userId(), orderResponse.items(),
                orderResponse.subtotal(), orderResponse.discountAmount(),
                orderResponse.shippingFee(), orderResponse.totalPrice(),
                orderResponse.couponCode(), orderResponse.status(),
                orderResponse.paymentStatus(), orderResponse.customerNote(),
                orderResponse.createdAt(), orderResponse.lastModifiedDate(),
                orderResponse.shippingAddress(), orderResponse.phoneNumber(),
                paymentUrl, orderResponse.paymentTransactions()
        );
    }
    private void checkRedisStockAvailable(OrderRequest request) {
        for (var item : request.orderItems()) {
            long availableStock = redisStockService.getStock(item.variantSku());
            if (availableStock < item.quantity()) {
                throw new RuntimeException(
                        "Insufficient stock for product: " + item.variantSku()
                        + ". Available: " + availableStock + ", Required: " + item.quantity());
            }
        }
    }
}
